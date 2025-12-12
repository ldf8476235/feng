package com.yyds.feng.lighter;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.yyds.feng"})
@MapperScan("com.yyds.feng.*.mapper")
public class LighterApplication {

    public static void main(String[] args) {
        SpringApplication.run(LighterApplication.class, args);
    }
}
