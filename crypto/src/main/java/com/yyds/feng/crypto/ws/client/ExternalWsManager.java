package com.yyds.feng.crypto.ws.client;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.yyds.feng.crypto.ws.config.CryptoWsProperties;
import com.yyds.feng.crypto.ws.service.PriceState;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Component
public class ExternalWsManager {

    @Autowired
    private CryptoWsProperties properties;

    @Autowired
    private PriceState priceState;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final AtomicBoolean variationalReconnectPending = new AtomicBoolean(false);
    private final AtomicBoolean standxReconnectPending = new AtomicBoolean(false);
    private final AtomicReference<ScheduledFuture<?>> variationalSubTask = new AtomicReference<>();

    private OkHttpClient client;
    private volatile WebSocket variationalSocket;
    private volatile WebSocket standxSocket;
    private volatile String variationalSubscribePayload;

    @PostConstruct
    public void start() {
        client = new OkHttpClient.Builder()
                .pingInterval(Duration.ofSeconds(properties.getPingIntervalSec()))
                .retryOnConnectionFailure(true)
                .build();

        variationalSubscribePayload = buildVariationalSubscribePayload();
        connectVariational();
        connectStandx();
    }

    @PreDestroy
    public void stop() {
        running.set(false);
        cancelVariationalSubscribe();
        closeSocket(variationalSocket, "variational shutdown");
        closeSocket(standxSocket, "standx shutdown");
        scheduler.shutdownNow();
        if (client != null) {
            client.dispatcher().executorService().shutdown();
            client.connectionPool().evictAll();
        }
    }

    private void connectVariational() {
        if (!running.get()) {
            return;
        }
        String url = properties.getVariationalUrl();
        if (isBlank(url)) {
            log.warn("variational ws url is empty, skip connect");
            return;
        }
        Request request = new Request.Builder().url(url).build();
        log.info("connecting variational ws: {}", url);
        variationalSocket = client.newWebSocket(request, new VariationalListener());
    }

    private void connectStandx() {
        if (!running.get()) {
            return;
        }
        String url = properties.getStandxUrl();
        if (isBlank(url)) {
            log.warn("standx ws url is empty, skip connect");
            return;
        }
        Request request = new Request.Builder().url(url).build();
        log.info("connecting standx ws: {}", url);
        standxSocket = client.newWebSocket(request, new StandxListener());
    }

    private void scheduleReconnect(AtomicBoolean pendingFlag, Runnable connectAction, String label) {
        if (!running.get()) {
            return;
        }
        if (!pendingFlag.compareAndSet(false, true)) {
            return;
        }
        scheduler.schedule(() -> {
            pendingFlag.set(false);
            if (!running.get()) {
                return;
            }
            log.info("reconnecting {} ws", label);
            connectAction.run();
        }, properties.getReconnectDelayMs(), TimeUnit.MILLISECONDS);
    }

    private void scheduleVariationalSubscribe(WebSocket webSocket) {
        cancelVariationalSubscribe();
        long intervalMs = properties.getVariationalSubscribeIntervalMs();
        if (intervalMs <= 0) {
            return;
        }
        ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(() -> {
            if (!running.get()) {
                return;
            }
            if (webSocket != variationalSocket) {
                return;
            }
            boolean sent = webSocket.send(variationalSubscribePayload);
            if (!sent) {
                log.debug("variational ws subscribe send failed");
            }
        }, intervalMs, intervalMs, TimeUnit.MILLISECONDS);
        variationalSubTask.set(future);
    }

    private void cancelVariationalSubscribe() {
        ScheduledFuture<?> future = variationalSubTask.getAndSet(null);
        if (future != null) {
            future.cancel(false);
        }
    }

    private void closeSocket(WebSocket socket, String reason) {
        if (socket != null) {
            try {
                socket.close(1000, reason);
            } catch (Exception e) {
                log.debug("ws close error: {}", reason, e);
            }
        }
    }

    private String buildVariationalSubscribePayload() {
        JSONObject payload = new JSONObject();
        payload.put("action", "subscribe");
        JSONArray instruments = new JSONArray();
        JSONObject instrument = new JSONObject();
        instrument.put("underlying", properties.getVariationalUnderlying());
        instrument.put("instrument_type", properties.getVariationalInstrumentType());
        instrument.put("settlement_asset", properties.getVariationalSettlementAsset());
        instrument.put("funding_interval_s", properties.getVariationalFundingIntervalSec());
        instruments.add(instrument);
        payload.put("instruments", instruments);
        return payload.toJSONString();
    }

    private String buildStandxAuthPayload(String token, String symbol) {
        if (isBlank(token)) {
            return null;
        }
        JSONObject auth = new JSONObject();
        JSONObject authBody = new JSONObject();
        authBody.put("token", token);
        JSONArray streams = new JSONArray();
        streams.add(buildStandxStream("order", null));
        streams.add(buildStandxStream("depth_book", symbol));
        streams.add(buildStandxStream("position", null));
        authBody.put("streams", streams);
        auth.put("auth", authBody);
        return auth.toJSONString();
    }

    private List<String> buildStandxSubscribePayloads(String token, String symbol) {
        List<String> payloads = new ArrayList<>();
        if (isBlank(token)) {
            payloads.add(buildStandxSubscribe("depth_book", symbol));
            return payloads;
        }
        payloads.add(buildStandxSubscribe("order", null));
        payloads.add(buildStandxSubscribe("depth_book", symbol));
        payloads.add(buildStandxSubscribe("position", null));
        return payloads;
    }

    private JSONObject buildStandxStream(String channel, String symbol) {
        JSONObject stream = new JSONObject();
        stream.put("channel", channel);
        if (!isBlank(symbol)) {
            stream.put("symbol", symbol);
        }
        return stream;
    }

    private String buildStandxSubscribe(String channel, String symbol) {
        JSONObject payload = new JSONObject();
        JSONObject subscribe = new JSONObject();
        subscribe.put("channel", channel);
        if (!isBlank(symbol)) {
            subscribe.put("symbol", symbol);
        }
        payload.put("subscribe", subscribe);
        return payload.toJSONString();
    }

    private Double parseDouble(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private class VariationalListener extends WebSocketListener {

        @Override
        public void onOpen(WebSocket webSocket, Response response) {
            log.info("variational ws opened");
            variationalSocket = webSocket;
            webSocket.send(variationalSubscribePayload);
            scheduleVariationalSubscribe(webSocket);
        }

        @Override
        public void onMessage(WebSocket webSocket, String text) {
            JSONObject data;
            try {
                data = JSON.parseObject(text);
            } catch (Exception e) {
                return;
            }
            if ("heartbeat".equals(data.getString("type"))) {
                return;
            }
            JSONObject pricing = data.getJSONObject("pricing");
            if (pricing == null) {
                return;
            }
            Double price = parseDouble(pricing.get("price"));
            if (price == null) {
                return;
            }
            priceState.updateVariational(price);
        }

        @Override
        public void onClosed(WebSocket webSocket, int code, String reason) {
            log.warn("variational ws closed: {} {}", code, reason);
            cancelVariationalSubscribe();
            scheduleReconnect(variationalReconnectPending, ExternalWsManager.this::connectVariational, "variational");
        }

        @Override
        public void onFailure(WebSocket webSocket, Throwable t, Response response) {
            log.warn("variational ws failure: {}", t.getMessage());
            cancelVariationalSubscribe();
            scheduleReconnect(variationalReconnectPending, ExternalWsManager.this::connectVariational, "variational");
        }
    }

    private class StandxListener extends WebSocketListener {

        @Override
        public void onOpen(WebSocket webSocket, Response response) {
            log.info("standx ws opened");
            standxSocket = webSocket;
            String token = properties.getStandxToken();
            String symbol = properties.getStandxSymbol();
            String authPayload = buildStandxAuthPayload(token, symbol);
            if (!isBlank(authPayload)) {
                webSocket.send(authPayload);
            }
            for (String payload : buildStandxSubscribePayloads(token, symbol)) {
                webSocket.send(payload);
            }
        }

        @Override
        public void onMessage(WebSocket webSocket, String text) {
            JSONObject data;
            try {
                data = JSON.parseObject(text);
            } catch (Exception e) {
                return;
            }
            if (!Objects.equals("depth_book", data.getString("channel"))) {
                return;
            }
            JSONObject payload = data.getJSONObject("data");
            if (payload == null) {
                return;
            }
            Double price = parseDouble(payload.get("last_price"));
            if (price == null) {
                return;
            }
            priceState.updateStandx(price);
        }

        @Override
        public void onClosed(WebSocket webSocket, int code, String reason) {
            log.warn("standx ws closed: {} {}", code, reason);
            scheduleReconnect(standxReconnectPending, ExternalWsManager.this::connectStandx, "standx");
        }

        @Override
        public void onFailure(WebSocket webSocket, Throwable t, Response response) {
            log.warn("standx ws failure: {}", t.getMessage());
            scheduleReconnect(standxReconnectPending, ExternalWsManager.this::connectStandx, "standx");
        }
    }
}
