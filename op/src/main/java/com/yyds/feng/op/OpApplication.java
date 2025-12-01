package com.yyds.feng.op;


import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@Slf4j
@ComponentScan(basePackages = {"com.yyds.feng"})
@MapperScan("com.yyds.feng.*.mapper")
@EnableScheduling
public class OpApplication {
    public static void main(String[] args) {
        log.info("项目启动成功！！！");
        SpringApplication.run(OpApplication.class, args);
    }
}
