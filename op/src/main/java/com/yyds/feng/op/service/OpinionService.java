package com.yyds.feng.op.service;
import cn.hutool.http.HttpRequest;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


@Slf4j
@Service
public class OpinionService {

    @Autowired
    @Qualifier("walletExecutor")
    private ThreadPoolExecutor walletExecutor;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private StringRedisTemplate redisTemplate;

    public List<JSONObject> fetchWalletBatch(List<String> wallets) {

        // 固定大小并保持顺序
        List<JSONObject> result = new CopyOnWriteArrayList<>(
                Collections.nCopies(wallets.size(), null)
        );

        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < wallets.size(); i++) {
            int index = i;
            String wallet = wallets.get(i);

            futures.add(walletExecutor.submit(() -> {
                JSONObject data = fetchWalletBatchWeb(wallet);
                data.put("number",index+1);
                result.set(index, data);
            }));
        }

        // 等待全部完成
        for (Future<?> future : futures) {
            try {
                future.get(30, TimeUnit.SECONDS);
            } catch (Exception e) {
                future.cancel(true);
                log.error("wallet task error: {}", e.getMessage());
            }
        }

        return result;
    }


    public JSONObject fetchWalletBatchWeb(String wallet) {

        JSONObject userDetail = new JSONObject();
        userDetail.put("address", wallet);
        userDetail.put("totalPoints", BigDecimal.ZERO);
        userDetail.put("netWorth", BigDecimal.ZERO);
        userDetail.put("totalVolume", BigDecimal.ZERO);
        userDetail.put("latestOrders", new ArrayList<>());
        userDetail.put("availableBalance", BigDecimal.ZERO);
        userDetail.put("userName", "");
        userDetail.put("portfolio", BigDecimal.ZERO);

        // 上周差值字段初始化
        userDetail.put("lastPoint", BigDecimal.ZERO);
        userDetail.put("lastVolume", BigDecimal.ZERO);
        userDetail.put("lastPortfolio", BigDecimal.ZERO);

        /* ----------------------------------------------
         * 1. 获取全部积分
         * ---------------------------------------------- */
        try {
            String url = "https://proxy.opinion.trade:8443/api/bsc/api/v2/leaderboard/"
                    + wallet + "?dataType=points&chainId=56";

            String resp = restTemplate.getForObject(url, String.class);
            JSONObject obj = JSONObject.parseObject(resp).getJSONObject("result");

            if (obj != null) {
                BigDecimal points = new BigDecimal(obj.getString("rankingValue"))
                        .setScale(3, RoundingMode.HALF_UP);

                userDetail.put("totalPoints", points);
            }
        } catch (Exception ignore) {
        }

        /* ----------------------------------------------
         * 2. 获取全部交易量 / 净值 / portfolio / balance
         * ---------------------------------------------- */
        try {
            String url = "https://proxy.opinion.trade:8443/api/bsc/api/v2/user/"
                    + wallet + "/profile?&chainId=56";

            String resp = restTemplate.getForObject(url, String.class);
            JSONObject obj = JSONObject.parseObject(resp).getJSONObject("result");

            if (obj != null) {

                userDetail.put("netWorth",
                        new BigDecimal(obj.getString("netWorth"))
                                .setScale(2, RoundingMode.HALF_UP)
                );

                userDetail.put("totalVolume",
                        new BigDecimal(obj.getString("Volume"))
                                .setScale(2, RoundingMode.HALF_UP)
                );

                JSONObject balance = obj.getJSONArray("balance").getJSONObject(0);
                userDetail.put("availableBalance",
                        new BigDecimal(balance.getString("balance"))
                                .setScale(2, RoundingMode.HALF_UP)
                );

                userDetail.put("userName", obj.getString("userName"));

                userDetail.put("portfolio",
                        new BigDecimal(obj.getString("totalProfit"))
                                .setScale(2, RoundingMode.HALF_UP)
                );
            }
        } catch (Exception ignore) {
        }

        /* ----------------------------------------------
         * 3. 查询上周快照（Redis）
         * ---------------------------------------------- */
        try {
            // weekNo 是字符串，要转 int（防止 null）
            String weekStr = redisTemplate.opsForValue().get("weekly:weekNo");
            if (weekStr != null) {
                int weekNo = Integer.parseInt(weekStr);  // 上周
                String keyLast = String.format("opinion:weekly:%s:%d", wallet, weekNo);
                if (redisTemplate.hasKey(keyLast)) {
                    Map<Object, Object> last = redisTemplate.opsForHash().entries(keyLast);
                    BigDecimal lastPoints = new BigDecimal((String) last.getOrDefault("deltaPoints", "0"));
                    userDetail.put("lastPoint",lastPoints.setScale(3, RoundingMode.HALF_UP));
                    BigDecimal totalPoints = new BigDecimal((String) last.getOrDefault("totalPoints", "0"));
                    BigDecimal lastVolume = new BigDecimal((String) last.getOrDefault("deltaVolume", "0"));
                    BigDecimal lastPortfolio = new BigDecimal((String) last.getOrDefault("delataPortfolio", "0"));
                    userDetail.put("lastVolume",lastVolume.setScale(2, RoundingMode.HALF_UP));
//                    userDetail.put("lastPortfolio",lastPortfolio.setScale(2, RoundingMode.HALF_UP));
                    BigDecimal delta = userDetail.getBigDecimal("totalPoints").subtract(totalPoints);
                    delta = delta.subtract(lastPoints);
                    if (delta.compareTo(BigDecimal.ZERO) > 0) {
                        // delta > 0
                        userDetail.put("lastPoint",delta.setScale(3, RoundingMode.HALF_UP));
                        redisTemplate.opsForHash().put(keyLast, "deltaPoints", delta.toString());
                    }
                } else {
                    try {
                        String url = "https://proxy.opinion.trade:8443/api/bsc/api/v2/leaderboard/"
                                + wallet + "?dataType=volume&chainId=56&period=7";

                        String resp = restTemplate.getForObject(url, String.class);
                        JSONObject obj = JSONObject.parseObject(resp).getJSONObject("result");

                        if (obj != null) {
                            BigDecimal points = new BigDecimal(obj.getString("rankingValue"))
                                    .setScale(3, RoundingMode.HALF_UP);

                            userDetail.put("lastVolume", points);
                        }
                    } catch (Exception ignore) {
                    }try {

                        String url = "https://proxy.opinion.trade:8443/api/bsc/api/v2/leaderboard/"
                                + wallet + "?dataType=points&chainId=56&period=7";

                        String resp = restTemplate.getForObject(url, String.class);
                        JSONObject obj = JSONObject.parseObject(resp).getJSONObject("result");

                        if (obj != null) {
                            BigDecimal points = new BigDecimal(obj.getString("rankingValue"))
                                    .setScale(3, RoundingMode.HALF_UP);

                            userDetail.put("lastPoint", points);
                        }
                    } catch (Exception ignore) {
                    }
                }
            }
        } catch (Exception e) {
            // 不写日志，防止刷屏
        }
        return userDetail;
    }

    public JSONObject fetchSingleWallet(String wallet) {

        JSONObject userDetail = new JSONObject();
        userDetail.put("address", wallet);
        userDetail.put("totalPoints", BigDecimal.ZERO);
        userDetail.put("netWorth", BigDecimal.ZERO);
        userDetail.put("totalVolume", BigDecimal.ZERO);
        userDetail.put("latestOrders", new ArrayList<>());
        userDetail.put("availableBalance", BigDecimal.ZERO);
        userDetail.put("userName", "");
        userDetail.put("portfolio", BigDecimal.ZERO);

        // 上周差值字段初始化
        userDetail.put("lastPoint", BigDecimal.ZERO);
        userDetail.put("lastVolume", BigDecimal.ZERO);
        userDetail.put("lastPortfolio", BigDecimal.ZERO);

        /* ----------------------------------------------
         * 1. 获取全部积分
         * ---------------------------------------------- */
        try {
            String url = "https://proxy.opinion.trade:8443/api/bsc/api/v2/leaderboard/"
                    + wallet + "?dataType=points&chainId=56";

            String resp = restTemplate.getForObject(url, String.class);
            JSONObject obj = JSONObject.parseObject(resp).getJSONObject("result");

            if (obj != null) {
                BigDecimal points = new BigDecimal(obj.getString("rankingValue"))
                        .setScale(3, RoundingMode.HALF_UP);

                userDetail.put("totalPoints", points);
            }
        } catch (Exception ignore) {
        }

        /* ----------------------------------------------
         * 2. 获取全部交易量 / 净值 / portfolio / balance
         * ---------------------------------------------- */
        try {
            String url = "https://proxy.opinion.trade:8443/api/bsc/api/v2/user/"
                    + wallet + "/profile?&chainId=56";

            String resp = restTemplate.getForObject(url, String.class);
            JSONObject obj = JSONObject.parseObject(resp).getJSONObject("result");

            if (obj != null) {

                userDetail.put("netWorth",
                        new BigDecimal(obj.getString("netWorth"))
                                .setScale(2, RoundingMode.HALF_UP)
                );

                userDetail.put("totalVolume",
                        new BigDecimal(obj.getString("Volume"))
                                .setScale(2, RoundingMode.HALF_UP)
                );

                JSONObject balance = obj.getJSONArray("balance").getJSONObject(0);
                userDetail.put("availableBalance",
                        new BigDecimal(balance.getString("balance"))
                                .setScale(2, RoundingMode.HALF_UP)
                );

                userDetail.put("userName", obj.getString("userName"));

                userDetail.put("portfolio",
                        new BigDecimal(obj.getString("totalProfit"))
                                .setScale(2, RoundingMode.HALF_UP)
                );
            }
        } catch (Exception ignore) {
        }

        /* ----------------------------------------------
         * 3. 查询上周快照（Redis）
         * ---------------------------------------------- */
        try {
            // weekNo 是字符串，要转 int（防止 null）
            String weekStr = redisTemplate.opsForValue().get("weekly:weekNo");
            if (weekStr != null) {
                int weekNo = Integer.parseInt(weekStr) - 1;  // 上周
                String keyLast = String.format("opinion:weekly:%s:%d", wallet, weekNo);
                if (redisTemplate.hasKey(keyLast)) {
                    Map<Object, Object> last = redisTemplate.opsForHash().entries(keyLast);
                    //交易量
                    BigDecimal lastVolume = new BigDecimal((String) last.getOrDefault("deltaVolume", "0"));
                    BigDecimal nowVolume = userDetail.getBigDecimal("totalVolume");
                    userDetail.put("lastVolume", nowVolume.subtract(lastVolume).setScale(2, RoundingMode.HALF_UP));

                    //盈亏
                    BigDecimal lastPortfolio = new BigDecimal((String) last.getOrDefault("delataPortfolio", "0"));
                    BigDecimal nowPortfolio = userDetail.getBigDecimal("portfolio");
                    userDetail.put("lastPortfolio", nowPortfolio.subtract(lastPortfolio).setScale(2, RoundingMode.HALF_UP));
                } else {
                    try {
                        String url = "https://proxy.opinion.trade:8443/api/bsc/api/v2/leaderboard/"
                                + wallet + "?dataType=volume&chainId=56&period=7";

                        String resp = restTemplate.getForObject(url, String.class);
                        JSONObject obj = JSONObject.parseObject(resp).getJSONObject("result");

                        if (obj != null) {
                            BigDecimal points = new BigDecimal(obj.getString("rankingValue"))
                                    .setScale(3, RoundingMode.HALF_UP);

                            userDetail.put("lastVolume", points);
                        }
                    } catch (Exception ignore) {
                    }
                    try {
                        String url = "https://proxy.opinion.trade:8443/api/bsc/api/v2/leaderboard/"
                                + wallet + "?dataType=profit&chainId=56&period=7";

                        String resp = restTemplate.getForObject(url, String.class);
                        JSONObject obj = JSONObject.parseObject(resp).getJSONObject("result");

                        if (obj != null) {
                            BigDecimal points = new BigDecimal(obj.getString("rankingValue"))
                                    .setScale(3, RoundingMode.HALF_UP);

                            userDetail.put("lastPortfolio", points);
                        }
                    } catch (Exception ignore) {
                    }
                }
            }
        } catch (Exception e) {
            // 不写日志，防止刷屏
        }
        return userDetail;
    }
}
