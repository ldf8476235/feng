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

    /** 本周交易量增量 */
    private BigDecimal deltaVolume;

    /** 本周积分增量 */
    private BigDecimal deltaPoints;

    /** 本周盈亏增量 */
    private BigDecimal deltaProfit;

    /** 快照时间 */
    private LocalDateTime snapshotTime;
}
