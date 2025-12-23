package com.yyds.feng.alpha.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_default_source")
public class UserDefaultSource {
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;
    private String username;
    private Integer defaultSource;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
