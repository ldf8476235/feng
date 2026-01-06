package com.yyds.feng.crypto;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.yyds.feng"})
@MapperScan("com.yyds.feng.*.mapper")
public class CryptoApplication {
    public static void main(String[] args) {
        SpringApplication.run(CryptoApplication.class, args);
    }
}
