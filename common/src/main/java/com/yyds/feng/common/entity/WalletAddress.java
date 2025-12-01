package com.yyds.feng.common.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("wallet_address")
public class WalletAddress {
    private Long id;
    private String wallet;
}
