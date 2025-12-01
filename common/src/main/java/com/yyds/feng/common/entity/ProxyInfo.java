package com.yyds.feng.common.entity;

import lombok.Data;

@Data
public class ProxyInfo {
    private Integer id;
    private String host;
    private Integer port;
    private String username;
    private String password;
    private Integer enable;
}