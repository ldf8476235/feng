package com.yyds.feng.op.config;

import com.yyds.feng.common.entity.ProxyInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 代理池 - 轮询方式选择代理
 */
@Slf4j
@Component
public class ProxyPool {

    private final List<ProxyInfo> proxies = new ArrayList<>();
    private final AtomicInteger index = new AtomicInteger(0);

    @PostConstruct
    public void init() {
        // 初始化代理列表
        String[] proxyConfigs = {
                "2.59.149.205:44001:fzK6954c41616e77:ga6nrR82Hqb9wLKU0b"
//                "82.22.89.194:7900:hgbgvnnv:2xqc2ff091er",
//                "82.22.69.108:7315:hgbgvnnv:2xqc2ff091er",
//                "82.22.89.83:7789:hgbgvnnv:2xqc2ff091er",
//                "82.22.93.193:7900:hgbgvnnv:2xqc2ff091er",
//                "82.22.89.10:7716:hgbgvnnv:2xqc2ff091er",
//                "82.22.89.251:7957:hgbgvnnv:2xqc2ff091er",
//                "82.22.89.127:7833:hgbgvnnv:2xqc2ff091er",
//                "82.22.73.44:7250:hgbgvnnv:2xqc2ff091er",
//                "82.22.89.35:7741:hgbgvnnv:2xqc2ff091er"
//                "82.22.73.91:7297:hgbgvnnv:2xqc2ff091er",
//                "82.22.89.24:7730:hgbgvnnv:2xqc2ff091er",
//                "82.22.69.75:7282:hgbgvnnv:2xqc2ff091er",
//                "82.22.89.122:7828:hgbgvnnv:2xqc2ff091er",
//                "82.22.93.246:7953:hgbgvnnv:2xqc2ff091er",
//                "82.22.89.195:7901:hgbgvnnv:2xqc2ff091er",
//                "82.22.93.219:7926:hgbgvnnv:2xqc2ff091er",
//                "82.22.93.3:7710:hgbgvnnv:2xqc2ff091er",
//                "82.22.69.89:7296:hgbgvnnv:2xqc2ff091er",
//                "82.22.69.208:7415:hgbgvnnv:2xqc2ff091er"
        };

        for (String config : proxyConfigs) {
            String[] parts = config.split(":");
            ProxyInfo proxy = new ProxyInfo();
            proxy.setHost(parts[0]);
            proxy.setPort(Integer.parseInt(parts[1]));
            proxy.setUsername(parts[2]);
            proxy.setPassword(parts[3]);
            proxy.setEnable(1);
            proxies.add(proxy);
        }

        log.info("代理池初始化完成，共 {} 个代理", proxies.size());
    }

    /**
     * 轮询获取下一个代理
     */
    public ProxyInfo getNextProxy() {
        int idx = Math.abs(index.getAndIncrement() % proxies.size());
        return proxies.get(idx);
    }

    /**
     * 获取代理数量
     */
    public int size() {
        return proxies.size();
    }
}
