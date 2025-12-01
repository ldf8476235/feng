package com.yyds.feng.common.util;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ProxyUtil {
    private String host;
    private int port;
    private String username;
    private String password;
}

