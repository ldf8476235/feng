package com.yyds.feng.op.config;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yyds.feng.common.entity.ProxyInfo;
import com.yyds.feng.op.mapper.ProxyMapper;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;

//@Component
public class ProxyPool {
    private final ProxyMapper proxyMapper;
    private final List<ProxyInfo> proxyList = new CopyOnWriteArrayList<>();

//    @Autowired
    public ProxyPool(ProxyMapper proxyMapper) {
        this.proxyMapper = proxyMapper;
        refresh();
    }

    public synchronized void refresh() {
        List<ProxyInfo> list = proxyMapper.selectList(
                new QueryWrapper<ProxyInfo>().eq("enable", 1)
        );
        proxyList.clear();
        proxyList.addAll(list);
        System.out.println("代理池加载数量：" + proxyList.size());
    }

    public ProxyInfo randomProxy() {
        if (proxyList.isEmpty()) return null;
        int i = ThreadLocalRandom.current().nextInt(proxyList.size());
        return proxyList.get(i);
    }
}
