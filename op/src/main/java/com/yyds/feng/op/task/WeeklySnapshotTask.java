package com.yyds.feng.op.task;

import com.alibaba.fastjson.JSONObject;
import com.yyds.feng.common.entity.WalletAddress;
import com.yyds.feng.common.entity.WalletWeeklySnapshot;

import com.yyds.feng.op.mapper.WalletAddressMapper;
import com.yyds.feng.op.mapper.WalletWeeklySnapshotMapper;
import com.yyds.feng.op.service.OpinionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@EnableScheduling
public class WeeklySnapshotTask {

    @Autowired
    private WalletAddressMapper walletAddressMapper;

    @Autowired
    private WalletWeeklySnapshotMapper snapshotMapper;

    @Autowired
    private OpinionService opinionService;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Resource
    @Qualifier("walletExecutor")
    private ThreadPoolExecutor walletExecutor;

    /**
     * 每周日 7:30 执行（北京时间）
     */
    @Scheduled(cron = "0 0 8 ? * SUN", zone = "Asia/Shanghai")
//    @PostConstruct
    public void snapshotJob() {
        log.info("=== Weekly Snapshot Task Start ===");

        // 获取当前周号
        String weekStr = redisTemplate.opsForValue().get("weekly:weekNo");
        int thisWeek = (weekStr == null ? 6 : Integer.parseInt(weekStr) + 1);
        redisTemplate.opsForValue().set("weekly:weekNo", String.valueOf(thisWeek));

        // 获取全部钱包列表
        List<String> wallets = walletAddressMapper.selectAllWallets();
        log.info("本次快照钱包数量：{}", wallets.size());

        List<WalletWeeklySnapshot> snapshots = new CopyOnWriteArrayList<>();
        List<Future<?>> futures = new ArrayList<>();

        for (String wallet : wallets) {
            futures.add(walletExecutor.submit(() -> processSingleWallet(wallet, thisWeek, snapshots)));
        }

        // 等待全部结束
        for (Future<?> f : futures) {
            try {
                f.get(3600, TimeUnit.SECONDS);
            } catch (Exception e) {
                log.error("快照任务超时或异常：{}", e.getMessage());
            }
        }

        // 批量插入数据库
        if (!snapshots.isEmpty()) {
            snapshotMapper.insertBatch(snapshots);
        }

        log.info("=== Weekly Snapshot Task End ===");
    }


    /** 单个钱包快照处理 */
    private void processSingleWallet(String wallet, int weekNo, List<WalletWeeklySnapshot> snapshots) {

        JSONObject current = opinionService.fetchSingleWallet(wallet);

        // 本周累计值（totalXXX）
        BigDecimal totalPoints = current.getBigDecimal("totalPoints");
        BigDecimal totalVolume = current.getBigDecimal("totalVolume");
        BigDecimal totalNetWorth = current.getBigDecimal("netWorth");
        BigDecimal totalPortfolio = current.getBigDecimal("portfolio");


        BigDecimal deltaVolume = current.getBigDecimal("lastVolume");
        BigDecimal deltaNetWorth = current.getBigDecimal("netWorth");
        BigDecimal deltaPortfolio = current.getBigDecimal("lastPortfolio");

        // 上周累计值（从 Redis 取）
//        int lastWeek = weekNo - 1;
//        String keyLast = String.format("opinion:weekly:%s:%d", wallet, lastWeek);
//        Map<Object, Object> last = redisTemplate.opsForHash().entries(keyLast);
//
//        BigDecimal lastPoints = new BigDecimal((String) last.getOrDefault("totalPoints", "0"));
//        BigDecimal lastVolume = new BigDecimal((String) last.getOrDefault("totalVolume", "0"));
//        BigDecimal lastNetWorth = new BigDecimal((String) last.getOrDefault("totalNetWorth", "0"));
//        BigDecimal lastPortfolio = new BigDecimal((String) last.getOrDefault("totalPortfolio", "0"));
//
//        // 增量 deltaXXX
//        BigDecimal deltaPoints = totalPoints.subtract(lastPoints);
//        BigDecimal deltaVolume = totalVolume.subtract(lastVolume);
//        BigDecimal deltaNetWorth = totalNetWorth.subtract(lastNetWorth);
//        BigDecimal deltaPortfolio = totalPortfolio.subtract(lastPortfolio);

        // 写 Redis（为下周比较用）
        String redisKey = String.format("opinion:weekly:%s:%d", wallet, weekNo);
        redisTemplate.opsForHash().put(redisKey, "totalPoints", totalPoints.toString());
        redisTemplate.opsForHash().put(redisKey, "totalVolume", totalVolume.toString());
        redisTemplate.opsForHash().put(redisKey, "totalNetWorth", totalNetWorth.toString());
        redisTemplate.opsForHash().put(redisKey, "totalPortfolio", totalPortfolio.toString());
        redisTemplate.opsForHash().put(redisKey, "deltaVolume", deltaVolume.toString());
        redisTemplate.opsForHash().put(redisKey, "deltaPoints", new BigDecimal(0).toString());

        // 组装 MySQL 快照实体
        WalletWeeklySnapshot s = new WalletWeeklySnapshot();
        s.setWallet(wallet);
        s.setWeekNo(weekNo);
        s.setTotalPoints(totalPoints);
        s.setTotalVolume(totalVolume);
        s.setTotalNetWorth(totalNetWorth);
        s.setTotalPortfolio(totalPortfolio);
        s.setDeltaPoints(new BigDecimal(0));
        s.setDeltaVolume(deltaVolume);
        s.setDeltaNetWorth(deltaNetWorth);
        s.setDeltaPortfolio(deltaPortfolio);
        s.setSnapshotTime(LocalDateTime.now());

        snapshots.add(s);
    }
}
