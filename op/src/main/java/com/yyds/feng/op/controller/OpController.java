package com.yyds.feng.op.controller;

import cn.hutool.http.HttpRequest;
import com.alibaba.fastjson.JSONObject;
import com.yyds.feng.common.entity.ProxyInfo;
import com.yyds.feng.common.util.R;
import com.yyds.feng.op.config.ProxyPool;
import com.yyds.feng.op.service.OpinionService;
import com.yyds.feng.op.service.WalletStoreService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

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
    public R getData(@RequestBody WalletRequest request) {
        List<String> wallets =
                (request != null && request.getWallets() != null)
                        ? request.getWallets()
                        : new ArrayList<>();
        // 限制最大 200
        if (wallets.size() > 200) {
            wallets = wallets.subList(0, 200);
        }
        log.info("获取钱包数据 {}", wallets);
        // 异步存数据库
        for (String wallet : wallets) {
            storeWalletExecutor.submit(() -> walletStoreService.addWalletAsync(wallet));
        }

        // ⭐ 使用 Service 获取钱包数据
        List<JSONObject> result = opinionService.fetchWalletBatch(wallets);

        log.info("响应数据 {}", result);
        return R.ok().put("data", result);
    }


    @Data
    public static class WalletRequest {
        private List<String> wallets;
    }
}
