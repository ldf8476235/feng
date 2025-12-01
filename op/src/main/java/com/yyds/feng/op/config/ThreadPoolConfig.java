package com.yyds.feng.op.config;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Configuration
public class ThreadPoolConfig {

    @Bean(name = "walletExecutor")
    public ThreadPoolExecutor walletExecutor() {
        int core = 32;
        int max = 128;
        int queueSize = 2000;
        long keepAlive = 60L;

        return new ThreadPoolExecutor(
                core,
                max,
                keepAlive,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(queueSize),
                new ThreadFactoryBuilder().setNameFormat("wallet-pool-%d").build(),
                new ThreadPoolExecutor.CallerRunsPolicy() // 自动限流
        );
    }

    @Bean("storeWalletExecutor")
    public ThreadPoolExecutor storeWalletExecutor() {
        return new ThreadPoolExecutor(
                4,                // core
                8,                // max
                60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(10000),
                new ThreadFactoryBuilder().setNameFormat("wallet-store-%d").build(),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }
}
