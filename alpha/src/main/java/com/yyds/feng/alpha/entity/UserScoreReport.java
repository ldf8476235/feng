package com.yyds.feng.alpha.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("user_score_report")
public class UserScoreReport {
    private Long id;
    private String username;
    private Integer source;
    private Double balance;
    private Boolean airdrop;
    private String reportDate;   // MM-DD
}
