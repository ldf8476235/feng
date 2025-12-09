package com.yyds.feng.alpha.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * 缓存空投列表数据，定时从三方接口拉取。
 */
@Slf4j
@Service
public class AirdropCacheService {

    private static final String AIRDROP_CACHE_KEY = "alpha:airdrop:list";
    private static final String AIRDROP_API_URL = "https://alpha123.uk/api/data?fresh=1";

    @Resource
    private RestTemplate restTemplate;

    @Resource
    private RedisTemplate<String, String> redisTemplate;

    @PostConstruct
    public void init() {
        refreshCache();
    }

    /**
     * 每5分钟刷新一次缓存。
     */
    @Scheduled(cron = "0 */5 * * * ?")
    public void refreshAirdropCache() {
        refreshCache();
    }

    /**
     * 手动刷新缓存，返回最新数据。
     */
    public String refreshCache() {
        try {
            String result = restTemplate.getForObject(AIRDROP_API_URL, String.class);
            if (result != null && !result.isEmpty()) {
                redisTemplate.opsForValue().set(AIRDROP_CACHE_KEY, result, 10, TimeUnit.MINUTES);
                log.info("Airdrop cache refreshed. Payload length={}", result.length());
            } else {
                log.warn("Airdrop API returned empty response, cache not updated");
            }
            return result;
        } catch (Exception e) {
            log.error("Failed to refresh airdrop cache from API", e);
            return null;
        }
    }

    public String getCachedAirdropData() {
        return redisTemplate.opsForValue().get(AIRDROP_CACHE_KEY);
    }
}
