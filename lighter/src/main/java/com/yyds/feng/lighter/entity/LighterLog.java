package com.yyds.feng.lighter.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("lighter_log")
public class LighterLog {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String content;

    /**
     * 日期（MM-DD）
     */
    @TableField("date_md")
    private String dateMd;

    /**
     * 时间（HH:MM）
     */
    @TableField("time_hm")
    private String timeHm;
}
