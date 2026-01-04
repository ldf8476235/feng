package com.yyds.feng.alpha.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("user_score_report_vd")
public class UserScoreReportVd {
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;
    private String username;
    private Integer source;
    private Double balance;
    private Integer airdrop;
    /** Airdrop count, default 0 */
    private Integer airdropCount;
    private String reportDate;   // MM-DD
}
