package com.yyds.feng.op.task;

import com.yyds.feng.common.entity.WalletWeeklySnapshot;
import com.yyds.feng.op.mapper.WalletAddressMapper;
import com.yyds.feng.op.mapper.WalletWeeklySnapshotMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class SaveRedisToMysql {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private WalletAddressMapper walletAddressMapper;

    @Autowired
    private WalletWeeklySnapshotMapper snapshotMapper;

    private static final int BATCH_SIZE = 100;

    // @PostConstruct
    public void run() {
        log.info("=== SaveRedisToMysql Task Start ===");

        String weekStr = redisTemplate.opsForValue().get("weekly:lastWeekNo");
        int lastWeek = (weekStr == null ? 6 : Integer.parseInt(weekStr));
        log.info("Processing weekNo: {}", lastWeek);

        List<String> wallets = walletAddressMapper.selectWalletByNoStat(lastWeek);
        log.info("Found {} wallets to process", wallets.size());

        List<WalletWeeklySnapshot> batch = new ArrayList<>();
        int successCount = 0;

        for (String wallet : wallets) {
            String redisKey = String.format("opinion:weekly:%s:%d", wallet, lastWeek);

            if (!Boolean.TRUE.equals(redisTemplate.hasKey(redisKey))) {
                continue;
            }

            Map<Object, Object> data = redisTemplate.opsForHash().entries(redisKey);
            if (data.isEmpty()) {
                continue;
            }

            WalletWeeklySnapshot snapshot = new WalletWeeklySnapshot();
            snapshot.setWallet(wallet);
            snapshot.setWeekNo(lastWeek);
            snapshot.setDeltaPoints(parseBigDecimal(data.get("deltaPoint")));
            snapshot.setDeltaVolume(parseBigDecimal(data.get("deltaVolume")));
            snapshot.setDeltaProfit(parseBigDecimal(data.get("deltaProfit")));
            snapshot.setSnapshotTime(LocalDateTime.now());

            batch.add(snapshot);

            // 批量写入
            if (batch.size() >= BATCH_SIZE) {
                snapshotMapper.insertBatch(batch);
                successCount += batch.size();
                batch.clear();
            }
        }

        // 剩余数据写入
        if (!batch.isEmpty()) {
            snapshotMapper.insertBatch(batch);
            successCount += batch.size();
        }

        log.info("=== SaveRedisToMysql Task End, saved {} records ===", successCount);
    }

    private BigDecimal parseBigDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(value.toString());
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    /**
     * 每周日早上8点执行，将 weekly:lastWeekNo 加1
     */
    @Scheduled(cron = "0 0 8 ? * SUN", zone = "Asia/Shanghai")
    public void incrementWeekNo() {
        String key = "weekly:lastWeekNo";
        String weekStr = redisTemplate.opsForValue().get(key);
        int currentWeek = (weekStr == null ? 6 : Integer.parseInt(weekStr));
        int newWeek = currentWeek + 1;
        redisTemplate.opsForValue().set(key, String.valueOf(newWeek));
        log.info("Weekly task: incremented lastWeekNo from {} to {}", currentWeek, newWeek);
    }
}