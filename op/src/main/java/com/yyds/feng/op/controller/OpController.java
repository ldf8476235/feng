package com.yyds.feng.op.controller;

import com.alibaba.fastjson.JSONObject;
import com.yyds.feng.common.util.R;
import com.yyds.feng.op.service.OpinionService;
import com.yyds.feng.op.service.WalletStoreService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

@Slf4j
@RestController
@RequestMapping
public class OpController {

//    @Autowired
//    private ProxyPool proxyPool;
    @Autowired
    RestTemplate restTemplate;

    @Autowired
    @Qualifier("walletExecutor")
    private ThreadPoolExecutor walletExecutor;

    @Autowired
    @Qualifier("storeWalletExecutor")
    private ThreadPoolExecutor storeWalletExecutor;

    @Autowired
    private WalletStoreService walletStoreService;

    @Autowired
    private OpinionService opinionService;

    @PostMapping("/getData")
    public R getData(@RequestBody WalletRequest request, HttpServletRequest httpRequest) {

        List<String> wallets =
                (request != null && request.getWallets() != null)
                        ? request.getWallets()
                        : new ArrayList<>();
        // 限制最大 200
        if (wallets.size() > 200) {
            wallets = wallets.subList(0, 200);
        }
        String clientIp = getClientIp(httpRequest);
        log.info("Client IP: {} searching wallet---size {}",clientIp, wallets.size());
        // 异步存数据库
        for (String wallet : wallets) {
            storeWalletExecutor.submit(() -> walletStoreService.addWalletAsync(wallet));
        }

        long fetchStart = System.currentTimeMillis();
        // ⭐ 使用 Service 获取钱包数据
        List<JSONObject> result = opinionService.fetchWalletBatch(wallets);
        long fetchEnd = System.currentTimeMillis();

        log.info("{} wallet searchSuccess {} ms",wallets.size(), (fetchEnd - fetchStart));
        return R.ok().put("data", result);
    }

    @GetMapping("/getOrder")
    public Object getOrder(
            @RequestParam int limit,
            @RequestParam int chainId,
            @RequestParam int page,
            @RequestParam String walletAddress
    ) {
        log.info("getOrder limit={}, chainId={}, page={}, walletAddress={}",
                limit, chainId, page, walletAddress);

        // 调用 Service 获取数据
        Object result = opinionService.getOrder(limit, chainId, page, walletAddress);

        return result;
    }


    @Data
    public static class WalletRequest {
        private List<String> wallets;
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");

        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            // 多级反向代理情况下，第一个 IP 为真实 IP
            return ip.split(",")[0].trim();
        }

        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }

        return request.getRemoteAddr();
    }

}
