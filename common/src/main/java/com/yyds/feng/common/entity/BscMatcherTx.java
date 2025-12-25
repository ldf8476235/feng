package com.yyds.feng.common.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("bsc_matcher_tx")
public class BscMatcherTx {
    private Long id;
    private String txHash;
    private Long blockNumber;
    private LocalDateTime blockTime;
    private String date;
    private String address;
    private String side;
    private BigDecimal valueAmount;
    private LocalDateTime createdAt;

    @TableField(exist = false)
    private String fromAddress;

    @TableField(exist = false)
    private String toAddress;

    @TableField(exist = false)
    private String valueRaw;

    @TableField(exist = false)
    private Integer logIndex;
}
