package com.yyds.feng.common.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("bsc_matcher_sync_status")
public class BscMatcherSyncStatus {
    private Long id;
    private Long lastBlock;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
