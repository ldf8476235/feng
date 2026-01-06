package com.yyds.feng.crypto.ws.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "crypto.ws")
public class CryptoWsProperties {
    private String variationalUrl = "wss://omni-ws-server.prod.ap-northeast-1.variational.io/prices";
    private String standxUrl = "wss://perps.standx.com/ws-stream/v1";
    private String standxSymbol = "BTC-USD";
    private String standxToken = "";
    private String variationalUnderlying = "BTC";
    private String variationalInstrumentType = "perpetual_future";
    private String variationalSettlementAsset = "USDC";
    private int variationalFundingIntervalSec = 3600;
    private long reconnectDelayMs = 3000;
    private long pingIntervalSec = 30;
    private long variationalSubscribeIntervalMs = 200;
    private long reportIntervalMs = 200;

    public String getVariationalUrl() {
        return variationalUrl;
    }

    public void setVariationalUrl(String variationalUrl) {
        this.variationalUrl = variationalUrl;
    }

    public String getStandxUrl() {
        return standxUrl;
    }

    public void setStandxUrl(String standxUrl) {
        this.standxUrl = standxUrl;
    }

    public String getStandxSymbol() {
        return standxSymbol;
    }

    public void setStandxSymbol(String standxSymbol) {
        this.standxSymbol = standxSymbol;
    }

    public String getStandxToken() {
        return standxToken;
    }

    public void setStandxToken(String standxToken) {
        this.standxToken = standxToken;
    }

    public String getVariationalUnderlying() {
        return variationalUnderlying;
    }

    public void setVariationalUnderlying(String variationalUnderlying) {
        this.variationalUnderlying = variationalUnderlying;
    }

    public String getVariationalInstrumentType() {
        return variationalInstrumentType;
    }

    public void setVariationalInstrumentType(String variationalInstrumentType) {
        this.variationalInstrumentType = variationalInstrumentType;
    }

    public String getVariationalSettlementAsset() {
        return variationalSettlementAsset;
    }

    public void setVariationalSettlementAsset(String variationalSettlementAsset) {
        this.variationalSettlementAsset = variationalSettlementAsset;
    }

    public int getVariationalFundingIntervalSec() {
        return variationalFundingIntervalSec;
    }

    public void setVariationalFundingIntervalSec(int variationalFundingIntervalSec) {
        this.variationalFundingIntervalSec = variationalFundingIntervalSec;
    }

    public long getReconnectDelayMs() {
        return reconnectDelayMs;
    }

    public void setReconnectDelayMs(long reconnectDelayMs) {
        this.reconnectDelayMs = reconnectDelayMs;
    }

    public long getPingIntervalSec() {
        return pingIntervalSec;
    }

    public void setPingIntervalSec(long pingIntervalSec) {
        this.pingIntervalSec = pingIntervalSec;
    }

    public long getVariationalSubscribeIntervalMs() {
        return variationalSubscribeIntervalMs;
    }

    public void setVariationalSubscribeIntervalMs(long variationalSubscribeIntervalMs) {
        this.variationalSubscribeIntervalMs = variationalSubscribeIntervalMs;
    }

    public long getReportIntervalMs() {
        return reportIntervalMs;
    }

    public void setReportIntervalMs(long reportIntervalMs) {
        this.reportIntervalMs = reportIntervalMs;
    }
}
