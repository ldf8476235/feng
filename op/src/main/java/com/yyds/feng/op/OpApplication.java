package com.yyds.feng.op;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.yyds.feng"})
public class OpApplication {
    public static void main(String[] args) {
        SpringApplication.run(OpApplication.class, args);
    }
}
