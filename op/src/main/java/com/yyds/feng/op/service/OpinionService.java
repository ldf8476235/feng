package com.yyds.feng.op.service;

import com.alibaba.fastjson.JSONObject;
import com.yyds.feng.op.dto.BscMatcherTxDailySummary;
import com.yyds.feng.op.dto.BscMatcherTxDailySummaryRow;
import com.yyds.feng.op.dto.WalletChristmasData;
import com.yyds.feng.op.mapper.BscMatcherTxMapper;
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
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
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

    @Autowired
    private BscMatcherTxMapper bscMatcherTxMapper;

    private static final String BASE_URL = "https://proxy.opinion.trade:8443/api/bsc/api/v2/portfolio";
    private static final String PROFILE_URL_PREFIX = "https://proxy.opinion.trade:8443/api/bsc/api/v2/user/";
    private static final String MULTISIG_CACHE_PREFIX = "wallet:multisig:56:";

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

    public List<WalletChristmasData> fetchChristmasData(List<String> wallets) {
        List<String> originalWallets = new ArrayList<>();
        List<String> normalizedWallets = new ArrayList<>();
        if (wallets != null) {
            for (String wallet : wallets) {
                if (wallet == null) {
                    continue;
                }
                String trimmed = wallet.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                originalWallets.add(trimmed);
                normalizedWallets.add(trimmed.toLowerCase());
            }
        }
        if (normalizedWallets.isEmpty()) {
            return Collections.emptyList();
        }

        Set<String> uniqueWallets = new LinkedHashSet<>(normalizedWallets);
        Map<String, String> mappedByWallet = resolveMappedWallets(uniqueWallets);
        if (mappedByWallet.isEmpty()) {
            return buildChristmasResponse(originalWallets, normalizedWallets, Collections.emptyMap());
        }

        Set<String> mappedAddresses = new LinkedHashSet<>(mappedByWallet.values());
        if (mappedAddresses.isEmpty()) {
            return buildChristmasResponse(originalWallets, normalizedWallets, mappedByWallet);
        }

        List<BscMatcherTxDailySummaryRow> rows =
                bscMatcherTxMapper.selectDailySummaryByAddresses(new ArrayList<>(mappedAddresses));
        Map<String, List<BscMatcherTxDailySummary>> summaryByAddress = groupDailySummary(rows);

        return buildChristmasResponse(originalWallets, normalizedWallets, mappedByWallet, summaryByAddress);
    }

    private Map<String, String> resolveMappedWallets(Set<String> wallets) {
        Map<String, String> result = new ConcurrentHashMap<>();
        List<Future<?>> futures = new ArrayList<>();

        for (String wallet : wallets) {
            futures.add(walletExecutor.submit(() -> {
                try {
                    String mapped = resolveMappedWallet(wallet);
                    if (mapped != null && !mapped.isEmpty()) {
                        result.put(wallet, mapped);
                    }
                } catch (Exception e) {
                    log.warn("resolve mapped wallet failed: {} - {}", wallet, e.getMessage());
                }
            }));
        }

        for (Future<?> future : futures) {
            try {
                future.get(30, TimeUnit.SECONDS);
            } catch (Exception e) {
                future.cancel(true);
                log.error("resolve mapped wallet task error: {}", e.getMessage());
            }
        }
        return result;
    }

    private Map<String, List<BscMatcherTxDailySummary>> groupDailySummary(List<BscMatcherTxDailySummaryRow> rows) {
        Map<String, List<BscMatcherTxDailySummary>> grouped = new HashMap<>();
        if (rows == null || rows.isEmpty()) {
            return grouped;
        }
        for (BscMatcherTxDailySummaryRow row : rows) {
            if (row == null || row.getAddress() == null) {
                continue;
            }
            BscMatcherTxDailySummary summary = new BscMatcherTxDailySummary();
            summary.setDate(row.getDate());
            summary.setTotalCount(row.getTotalCount());
            summary.setTotalAmount(row.getTotalAmount());
            grouped.computeIfAbsent(row.getAddress(), key -> new ArrayList<>()).add(summary);
        }
        return grouped;
    }

    private List<WalletChristmasData> buildChristmasResponse(List<String> originalWallets,
                                                             List<String> normalizedWallets,
                                                             Map<String, String> mappedByWallet) {
        return buildChristmasResponse(originalWallets, normalizedWallets, mappedByWallet, Collections.emptyMap());
    }

    private List<WalletChristmasData> buildChristmasResponse(List<String> originalWallets,
                                                             List<String> normalizedWallets,
                                                             Map<String, String> mappedByWallet,
                                                             Map<String, List<BscMatcherTxDailySummary>> summaryByAddress) {
        List<WalletChristmasData> result = new ArrayList<>();
        if (originalWallets == null || originalWallets.isEmpty()
                || normalizedWallets == null || normalizedWallets.isEmpty()) {
            return result;
        }
        int size = Math.min(originalWallets.size(), normalizedWallets.size());
        for (int i = 0; i < size; i++) {
            String wallet = originalWallets.get(i);
            String normalized = normalizedWallets.get(i);
            WalletChristmasData item = new WalletChristmasData();
            item.setWallet(wallet);
            String mapped = mappedByWallet.get(normalized);
            item.setMappedWallet(mapped);
            if (mapped == null) {
                item.setDaily(Collections.emptyList());
            } else {
                item.setDaily(summaryByAddress.getOrDefault(mapped, Collections.emptyList()));
            }
            result.add(item);
        }
        return result;
    }

    private static final int MAX_RETRIES = 3;
    private static final int RETRY_DELAY_MS = 500;

    /**
     * 带重试的 HTTP GET 请求
     */
    private String fetchWithRetry(String url) {
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                return restTemplate.getForObject(url, String.class);
            } catch (Exception e) {
                log.warn("请求失败 (第 {} 次尝试): {} - {}", attempt, url, e.getMessage());
                if (attempt < MAX_RETRIES) {
                    try {
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        return null;
    }

    private String resolveMappedWallet(String wallet) {
        if (wallet == null || wallet.trim().isEmpty()) {
            return null;
        }
        String normalizedWallet = wallet.trim().toLowerCase();
        String cacheKey = MULTISIG_CACHE_PREFIX + normalizedWallet;
        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return cached.isEmpty() ? null : cached;
        }

        String mapped = fetchMappedWalletFromApi(normalizedWallet);
        if (mapped == null || mapped.trim().isEmpty()) {
            return null;
        }

        String normalizedMapped = mapped.trim().toLowerCase();
        redisTemplate.opsForValue().set(cacheKey, normalizedMapped);
        return normalizedMapped;
    }

    private String fetchMappedWalletFromApi(String wallet) {
        String url = PROFILE_URL_PREFIX + wallet + "/profile?&chainId=56";
        try {
            String resp = fetchWithRetry(url);
            if (resp == null) {
                return null;
            }
            JSONObject obj = JSONObject.parseObject(resp);
            if (obj == null) {
                return null;
            }
            JSONObject result = obj.getJSONObject("result");
            if (result == null) {
                return null;
            }
            JSONObject multiSigned = result.getJSONObject("multiSignedWalletAddress");
            if (multiSigned == null) {
                return null;
            }
            String mapped = multiSigned.getString("56");
            return (mapped == null || mapped.trim().isEmpty()) ? null : mapped;
        } catch (Exception e) {
            log.warn("fetch mapped wallet from api failed: {} - {}", wallet, e.getMessage());
            return null;
        }
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
//        try {
//            String url = "https://proxy.opinion.trade:8443/api/bsc/api/v2/leaderboard/"
//                    + wallet + "?dataType=points&chainId=56&period=0";
//
//            String resp = fetchWithRetry(url);
//            if (resp != null) {
//                JSONObject obj = JSONObject.parseObject(resp).getJSONObject("result");
//
//                if (obj != null) {
//                    BigDecimal points = new BigDecimal(obj.getString("rankingValue"))
//                            .setScale(3, RoundingMode.HALF_UP);
//
//                    userDetail.put("totalPoints", points);
//                }
//            }
//        } catch (Exception ignore) {
//        }

        /*
         * ----------------------------------------------
         * 2. 获取全部交易量 / 净值 / portfolio / balance
         * ----------------------------------------------
         */
        try {
            String url = "https://proxy.opinion.trade:8443/api/bsc/api/v2/user/"
                    + wallet + "/profile?&chainId=56";

            String resp = fetchWithRetry(url);
            if (resp != null) {
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
            }
        } catch (Exception ignore) {
        }
        // 上周积分
        String weekStr = redisTemplate.opsForValue().get("weekly:lastWeekNo");
        int lastWeek = (weekStr == null ? 6 : Integer.parseInt(weekStr));
        String keyLast = String.format("opinion:weekly:%s:%d", wallet, lastWeek);
//        try {
//            boolean needFetch = true;
//            if (redisTemplate.hasKey(keyLast)) {
//                Map<Object, Object> lastData = redisTemplate.opsForHash().entries(keyLast);
//                if (lastData.containsKey("deltaPoint")) {
//                    BigDecimal lastPoint = new BigDecimal((String) lastData.getOrDefault("deltaPoint", "0"));
//                    userDetail.put("lastPoint", lastPoint);
//                    needFetch = false;
//                }
//            }
//            if (needFetch) {
//                String url = "https://proxy.opinion.trade:8443/api/bsc/api/v2/leaderboard/"
//                        + wallet + "?dataType=points&chainId=56&period=7";
//
//                String resp = fetchWithRetry(url);
//                if (resp != null) {
//                    JSONObject obj = JSONObject.parseObject(resp).getJSONObject("result");
//
//                    if (obj != null) {
//                        BigDecimal points = new BigDecimal(obj.getString("rankingValue"))
//                                .setScale(3, RoundingMode.HALF_UP);
//
//                        userDetail.put("lastPoint", points);
//                        redisTemplate.opsForHash().put(keyLast, "deltaPoint", points.toString());
//                        redisTemplate.expire(keyLast, 7, TimeUnit.DAYS);
//                    }
//                }
//            }
//        } catch (Exception ignore) {
//        }
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
                resp = fetchWithRetry(url);
                if (resp != null) {
                    JSONObject obj = JSONObject.parseObject(resp).getJSONObject("result");
                    BigDecimal lastWeekVolume = calcLastWeekVolume(obj);
                    userDetail.put("lastVolume", lastWeekVolume);
                    redisTemplate.opsForHash().put(keyLast, "deltaVolume", lastWeekVolume.toString());

                    BigDecimal lastWeekProfit = calcLastWeekProfit(obj);
                    userDetail.put("lastProfit", lastWeekProfit);
                    redisTemplate.opsForHash().put(keyLast, "deltaProfit", lastWeekProfit.toString());
                    redisTemplate.expire(keyLast, 7, TimeUnit.DAYS);
                } else {
                    resp = "";
                }
            }
        } catch (Exception ignore) {
        }
        // 本周
        if (!needFetch) {
            String fetchedResp = fetchWithRetry(url);
            if (fetchedResp != null) {
                resp = fetchedResp;
            }
        }
        try {
            if (resp != null && !resp.isEmpty()) {
                JSONObject obj = JSONObject.parseObject(resp).getJSONObject("result");
                BigDecimal thisWeekVolume = calcThisWeekVolume(obj);
                userDetail.put("thisVolume", thisWeekVolume);

                BigDecimal thisWeekProfit = calcThisWeekProfit(obj);
                userDetail.put("thisProfit", thisWeekProfit);
            }
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

        int maxRetries = 2;
        Exception lastException = null;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
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
                lastException = e;
                log.warn("OP API 请求失败 (第 {} 次尝试): {}", attempt, e.getMessage());
                if (attempt < maxRetries) {
                    try {
                        Thread.sleep(500); // 等待 500ms 后重试
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }

        if (lastException != null) {
            log.error("Error calling OP API after {} retries: {}", maxRetries, lastException.getMessage(),
                    lastException);
        } else {
            log.error("Error calling OP API after {} retries, but no exception captured", maxRetries);
        }
        return Collections.emptyList();
    }
}
