package com.yyds.feng.op.service;

import com.yyds.feng.op.mapper.WalletAddressMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class WalletStoreService {

    private final WalletAddressMapper walletAddressMapper;
    private final StringRedisTemplate redis;

    // 批量队列
    private final ConcurrentLinkedQueue<String> walletQueue = new ConcurrentLinkedQueue<>();

    // 批处理参数
    private static final int BATCH_SIZE = 100; // 满 100 条就批量写入
    private static final int FLUSH_INTERVAL = 60; // 每 1s 检查一次

    // ============================
    // 后台定时任务：定时 flush 批量入库
    // ============================
    @PostConstruct
    public void initFlushTask() {
        Executors.newSingleThreadScheduledExecutor()
                .scheduleAtFixedRate(this::flushBatch, 2, FLUSH_INTERVAL, TimeUnit.SECONDS);

        log.info("Wallet batch insert scheduler started.");
    }

    public void addWalletAsync(String wallet) {
        String redisKey = "wallet:address:" + wallet.toLowerCase();

        Boolean firstSeen = redis.opsForValue().setIfAbsent(redisKey, "1", 7, TimeUnit.DAYS);
        if (Boolean.FALSE.equals(firstSeen)) {
            return; // 已存在，不入队列
        }

        walletQueue.add(wallet);
    }

    private synchronized void flushBatch() {
        int queueSize = walletQueue.size();
        if (queueSize == 0) {
            return; // 队列空，不写库
        }

        // 每次 flush 最多 100 条，但如果不满 100 条也会写
        List<String> batch = new ArrayList<>();

        while (!walletQueue.isEmpty() && batch.size() < BATCH_SIZE) {
            String wallet = walletQueue.poll();
            if (wallet != null) {
                batch.add(wallet);
            }
        }

        if (batch.isEmpty())
            return;

        try {
            int rows = walletAddressMapper.insertBatch(batch);
            // log.info("批量入库 {} 条钱包（成功 {} 条）", batch.size(), rows);
        } catch (Exception e) {
            log.error("insert wallet error：{}", e.getMessage());
        }
    }

    public void storeWallet(String wallet) {

        // Redis 标记不过期（可选）
        // redis.expire(redisKey, Duration.ofDays(365));

        // ================================
        // 2. MySQL 去重（INSERT IGNORE）
        // ================================
        try {
            String redisKey = "wallet:address:" + wallet.toLowerCase();
            log.info("入库");
            // ================================
            // 1. Redis 去重（SETNX）
            // ================================
            Boolean firstSeen = redis.opsForValue().setIfAbsent(redisKey, "1", 7, TimeUnit.DAYS);

            if (Boolean.FALSE.equals(firstSeen)) {
                // Redis 已经存在 → 不入库
                log.info("Redis 去重：跳过已存在的钱包 {}", wallet);
                return;
            }
            int rows = walletAddressMapper.insertIgnore(wallet);

            if (rows > 0) {
                log.info("钱包入库成功 {}", wallet);
            } else {
                log.info("MySQL 去重：钱包已存在 {}", wallet);
            }

        } catch (Exception e) {
            log.error("数据库写入失败 {} : {}", wallet, e.getMessage());
        }
    }
}
