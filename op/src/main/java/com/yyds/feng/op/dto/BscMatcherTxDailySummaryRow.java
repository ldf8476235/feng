package com.yyds.feng.op.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class BscMatcherTxDailySummaryRow {
    private String address;
    private String date;
    private Long totalCount;
    private BigDecimal totalAmount;
}
