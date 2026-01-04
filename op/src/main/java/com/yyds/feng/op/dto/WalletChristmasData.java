package com.yyds.feng.op.dto;

import lombok.Data;

import java.util.List;

@Data
public class WalletChristmasData {
    private String wallet;
    private String mappedWallet;
    private List<BscMatcherTxDailySummary> daily;
}
