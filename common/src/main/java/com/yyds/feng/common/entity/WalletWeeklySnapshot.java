package com.yyds.feng.common.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("wallet_weekly_snapshot")
public class WalletWeeklySnapshot {
    private Long id;

    /** 钱包地址 */
    private String wallet;

    /** 第几周 */
    private Integer weekNo;

    /** 累积交易量 */
    private BigDecimal totalVolume;

    /** 累积净值（可用余额） */
    private BigDecimal totalNetWorth;

    /** 累积积分 */
    private BigDecimal totalPoints;

    /** 累积盈亏（portfolio） */
    private BigDecimal totalPortfolio;

    /** 本周交易量增量 */
    private BigDecimal deltaVolume;

    /** 本周净值增量 */
    private BigDecimal deltaNetWorth;

    /** 本周积分增量 */
    private BigDecimal deltaPoints;

    /** 本周盈亏增量 */
    private BigDecimal deltaPortfolio;

    /** 快照时间 */
    private LocalDateTime snapshotTime;
}
