package com.yyds.feng.crypto.ws.service;

import org.springframework.stereotype.Component;

@Component
public class PriceState {

    private Double variationalPrice;
    private Double standxPrice;
    private long variationalTs;
    private long standxTs;

    public synchronized void updateVariational(double price) {
        this.variationalPrice = price;
        this.variationalTs = System.currentTimeMillis();
    }

    public synchronized void updateStandx(double price) {
        this.standxPrice = price;
        this.standxTs = System.currentTimeMillis();
    }

    public synchronized PriceSnapshot snapshot() {
        return new PriceSnapshot(variationalPrice, standxPrice, variationalTs, standxTs);
    }

    public static class PriceSnapshot {
        private final Double variationalPrice;
        private final Double standxPrice;
        private final long variationalTs;
        private final long standxTs;

        public PriceSnapshot(Double variationalPrice, Double standxPrice, long variationalTs, long standxTs) {
            this.variationalPrice = variationalPrice;
            this.standxPrice = standxPrice;
            this.variationalTs = variationalTs;
            this.standxTs = standxTs;
        }

        public Double getVariationalPrice() {
            return variationalPrice;
        }

        public Double getStandxPrice() {
            return standxPrice;
        }

        public long getVariationalTs() {
            return variationalTs;
        }

        public long getStandxTs() {
            return standxTs;
        }
    }
}
