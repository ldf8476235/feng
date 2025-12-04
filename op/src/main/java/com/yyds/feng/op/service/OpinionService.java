package com.yyds.feng.op.service;

import com.alibaba.fastjson.JSONObject;
import com.yyds.feng.op.utils.WeekUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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

    private static final String BASE_URL = "https://proxy.opinion.trade:8443/api/bsc/api/v2/portfolio";

    public List<JSONObject> fetchWalletBatch(List<String> wallets) {

        // 固定大小并保持顺序
        List<JSONObject> result = new CopyOnWriteArrayList<>(
                Collections.nCopies(wallets.size(), null));

        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < wallets.size(); i++) {
            int index = i;
            String wallet = wallets.get(i);

            futures.add(walletExecutor.submit(() -> {
                JSONObject data = fetchWalletBatchWeb(wallet);
                data.put("number", index + 1);
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

        /*
         * ----------------------------------------------
         * 1. 获取全部积分
         * ----------------------------------------------
         */
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

        /*
         * ----------------------------------------------
         * 2. 获取全部交易量 / 净值 / portfolio / balance
         * ----------------------------------------------
         */
        try {
            String url = "https://proxy.opinion.trade:8443/api/bsc/api/v2/user/"
                    + wallet + "/profile?&chainId=56";

            String resp = restTemplate.getForObject(url, String.class);
            JSONObject obj = JSONObject.parseObject(resp).getJSONObject("result");

            if (obj != null) {

                userDetail.put("netWorth",
                        new BigDecimal(obj.getString("netWorth"))
                                .setScale(2, RoundingMode.HALF_UP));

                userDetail.put("totalVolume",
                        new BigDecimal(obj.getString("Volume"))
                                .setScale(2, RoundingMode.HALF_UP));

                JSONObject balance = obj.getJSONArray("balance").getJSONObject(0);
                userDetail.put("availableBalance",
                        new BigDecimal(balance.getString("totalBalance"))
                                .setScale(2, RoundingMode.HALF_UP));

                userDetail.put("userName", obj.getString("userName"));

                userDetail.put("totalProfit",
                        new BigDecimal(obj.getString("totalProfit"))
                                .setScale(2, RoundingMode.HALF_UP));

                userDetail.put("totalPortfolio",
                        new BigDecimal(obj.getString("portfolio"))
                                .setScale(2, RoundingMode.HALF_UP));
            }
        } catch (Exception ignore) {
        }
        // 上周积分
        String weekStr = redisTemplate.opsForValue().get("weekly:lastWeekNo");
        int lastWeek = (weekStr == null ? 6 : Integer.parseInt(weekStr));
        String keyLast = String.format("opinion:weekly:%s:%d", wallet, lastWeek);
        try {
            boolean needFetch = true;
            if (redisTemplate.hasKey(keyLast)) {
                Map<Object, Object> lastData = redisTemplate.opsForHash().entries(keyLast);
                if (lastData.containsKey("deltaPoint")) {
                    BigDecimal lastPoint = new BigDecimal((String) lastData.getOrDefault("deltaPoint", "0"));
                    userDetail.put("lastPoint", lastPoint);
                    needFetch = false;
                }
            }
            if (needFetch) {
                String url = "https://proxy.opinion.trade:8443/api/bsc/api/v2/leaderboard/"
                        + wallet + "?dataType=points&chainId=56&period=7";

                String resp = restTemplate.getForObject(url, String.class);
                JSONObject obj = JSONObject.parseObject(resp).getJSONObject("result");

                if (obj != null) {
                    BigDecimal points = new BigDecimal(obj.getString("rankingValue"))
                            .setScale(3, RoundingMode.HALF_UP);

                    userDetail.put("lastPoint", points);
                    redisTemplate.opsForHash().put(keyLast, "deltaPoint", points.toString());
                    redisTemplate.expire(keyLast, 7, TimeUnit.DAYS);
                }
            }
        } catch (Exception ignore) {
        }
        // 上周交易量 与 盈亏
        boolean needFetch = true;
        String resp = "";
        String url = "https://proxy.opinion.trade:8443/api/bsc/api/v2/user/"
                + wallet + "/snapshots?chainId=56&period=15";
        try {
            if (redisTemplate.hasKey(keyLast)) {
                Map<Object, Object> lastData = redisTemplate.opsForHash().entries(keyLast);
                if (lastData.containsKey("deltaVolume") && lastData.containsKey("deltaProfit")) {
                    BigDecimal lastVolume = new BigDecimal((String) lastData.getOrDefault("deltaVolume", "0"));
                    userDetail.put("lastVolume", lastVolume);

                    BigDecimal lastProfit = new BigDecimal((String) lastData.getOrDefault("deltaProfit", "0"));
                    userDetail.put("lastProfit", lastProfit);
                    needFetch = false;
                }
            }
            if (needFetch) {
                resp = restTemplate.getForObject(url, String.class);
                JSONObject obj = JSONObject.parseObject(resp).getJSONObject("result");
                BigDecimal lastWeekVolume = calcLastWeekVolume(obj);
                userDetail.put("lastVolume", lastWeekVolume);
                redisTemplate.opsForHash().put(keyLast, "deltaVolume", lastWeekVolume.toString());

                BigDecimal lastWeekProfit = calcLastWeekProfit(obj);
                userDetail.put("lastProfit", lastWeekProfit);
                redisTemplate.opsForHash().put(keyLast, "deltaProfit", lastWeekProfit.toString());
                redisTemplate.expire(keyLast, 7, TimeUnit.DAYS);
            }
        } catch (Exception ignore) {
        }
        // 本周
        if (!needFetch) {
            try {
                resp = restTemplate.getForObject(url, String.class);
            } catch (Exception e) {
            }
        }
        try {
            JSONObject obj = JSONObject.parseObject(resp).getJSONObject("result");
            BigDecimal thisWeekVolume = calcThisWeekVolume(obj);
            userDetail.put("thisVolume", thisWeekVolume);

            BigDecimal thisWeekProfit = calcThisWeekProfit(obj);
            userDetail.put("thisProfit", thisWeekProfit);
        } catch (Exception ignore) {

        }
        return userDetail;
    }

    public BigDecimal calcLastWeekVolume(JSONObject resultObj) {

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        // 从 WeekUtils 获取上周区间
        WeekUtils.WeekRange week = WeekUtils.getLastWeekRange();

        BigDecimal total = BigDecimal.ZERO;

        for (Object o : resultObj.getJSONArray("snapshots")) {
            JSONObject snap = (JSONObject) o;

            LocalDate date = LocalDate.parse(snap.getString("date"), fmt);

            // belong to last week
            if ((date.isEqual(week.start) || date.isAfter(week.start))
                    && (date.isEqual(week.end) || date.isBefore(week.end))) {

                BigDecimal vol = snap.getBigDecimal("volume");
                if (vol != null) {
                    total = total.add(vol);
                }
            }
        }

        return total.setScale(2, BigDecimal.ROUND_HALF_UP);
    }

    public BigDecimal calcThisWeekVolume(JSONObject resultObj) {

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        // 从 WeekUtils 获取上周区间
        WeekUtils.WeekRange week = WeekUtils.getLastWeekRange();

        BigDecimal total = BigDecimal.ZERO;

        for (Object o : resultObj.getJSONArray("snapshots")) {
            JSONObject snap = (JSONObject) o;

            LocalDate date = LocalDate.parse(snap.getString("date"), fmt);

            // belong to last week
            if (date.isAfter(week.end)) {
                BigDecimal vol = snap.getBigDecimal("volume");
                if (vol != null) {
                    total = total.add(vol);
                }
            }
        }

        return total.setScale(2, BigDecimal.ROUND_HALF_UP);
    }

    public BigDecimal calcLastWeekProfit(JSONObject resultObj) {

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        // 从 WeekUtils 获取上周区间
        WeekUtils.WeekRange week = WeekUtils.getLastWeekRange();

        BigDecimal total = BigDecimal.ZERO;

        for (Object o : resultObj.getJSONArray("snapshots")) {
            JSONObject snap = (JSONObject) o;

            LocalDate date = LocalDate.parse(snap.getString("date"), fmt);

            // belong to last week
            if ((date.isEqual(week.start) || date.isAfter(week.start))
                    && (date.isEqual(week.end) || date.isBefore(week.end))) {

                BigDecimal vol = snap.getBigDecimal("profit");
                if (vol != null) {
                    total = total.add(vol);
                }
            }
        }

        return total.setScale(2, BigDecimal.ROUND_HALF_UP);
    }

    public BigDecimal calcThisWeekProfit(JSONObject resultObj) {

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        // 从 WeekUtils 获取上周区间
        WeekUtils.WeekRange week = WeekUtils.getLastWeekRange();

        BigDecimal total = BigDecimal.ZERO;

        for (Object o : resultObj.getJSONArray("snapshots")) {
            JSONObject snap = (JSONObject) o;

            LocalDate date = LocalDate.parse(snap.getString("date"), fmt);

            // belong to last week
            if (date.isAfter(week.end)) {
                BigDecimal vol = snap.getBigDecimal("profit");
                if (vol != null) {
                    total = total.add(vol);
                }
            }
        }

        return total.setScale(2, BigDecimal.ROUND_HALF_UP);
    }

    public Object getOrder(int limit, int chainId, int page, String walletAddress) {

        // ⭐ 拼接完整调用 URL
        String url = String.format(
                "%s?limit=%d&chainId=%d&page=%d&walletAddress=%s",
                BASE_URL, limit, chainId, page, walletAddress);

        log.info("Calling OP third-party -> {}", url);

        try {
            // 发送 GET
            String resp = restTemplate.getForObject(url, String.class);

            if (resp == null) {
                log.error("OP API returned null");
                return Collections.emptyList();
            }

            // 解析 JSON
            JSONObject json = JSONObject.parseObject(resp);

            return json;

        } catch (Exception e) {
            log.error("Error calling OP API: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }
}
