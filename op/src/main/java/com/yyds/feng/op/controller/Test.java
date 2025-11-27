package com.yyds.feng.op.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
public class Test {

    @RequestMapping(value = "test")
    public String test() {
        System.out.println("1");
        return "1";
    }
}
