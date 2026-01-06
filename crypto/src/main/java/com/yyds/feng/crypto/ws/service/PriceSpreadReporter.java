package com.yyds.feng.crypto.ws.service;

import com.alibaba.fastjson.JSONObject;
import com.yyds.feng.crypto.ws.config.CryptoWsProperties;
import com.yyds.feng.crypto.ws.handler.PriceSpreadWebSocketHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Slf4j
@Component
public class PriceSpreadReporter {

    @Autowired
    private PriceState priceState;

    @Autowired
    private PriceSpreadWebSocketHandler webSocketHandler;

    @Autowired
    private CryptoWsProperties properties;

    private double spreadSum = 0.0;
    private long spreadCount = 0;
    private boolean waitingLogged = false;

    @Scheduled(fixedDelayString = "${crypto.ws.report-interval-ms:200}")
    public void report() {
        PriceState.PriceSnapshot snapshot = priceState.snapshot();
        if (snapshot.getVariationalPrice() == null || snapshot.getStandxPrice() == null) {
            if (!waitingLogged) {
                log.info("waiting for prices: variational={} standx={}",
                        snapshot.getVariationalPrice(), snapshot.getStandxPrice());
                waitingLogged = true;
            }
            return;
        }

        waitingLogged = false;
        double diff = snapshot.getStandxPrice() - snapshot.getVariationalPrice();
        double spread = Math.abs(diff);
        spreadSum += spread;
        spreadCount += 1;
        double avgSpread = spreadSum / spreadCount;

        long now = System.currentTimeMillis();
        long variationalAgeMs = snapshot.getVariationalTs() > 0 ? now - snapshot.getVariationalTs() : 0;
        long standxAgeMs = snapshot.getStandxTs() > 0 ? now - snapshot.getStandxTs() : 0;

        JSONObject payload = new JSONObject();
        payload.put("type", "spread");
        payload.put("symbol", properties.getStandxSymbol());
        payload.put("variational", snapshot.getVariationalPrice());
        payload.put("standx", snapshot.getStandxPrice());
        payload.put("diff", round2(diff));
        payload.put("spread", round2(spread));
        payload.put("avgSpread", round2(avgSpread));
        payload.put("timestamp", now);
        payload.put("variationalTs", snapshot.getVariationalTs());
        payload.put("standxTs", snapshot.getStandxTs());
        payload.put("variationalAgeMs", variationalAgeMs);
        payload.put("standxAgeMs", standxAgeMs);

        webSocketHandler.broadcast(payload.toJSONString());
    }

    private double round2(double value) {
        return BigDecimal.valueOf(value)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
