package com.yyds.feng.op.task;

import com.yyds.feng.op.mapper.WalletAddressMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Component
public class SaveRedisToMysql {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private WalletAddressMapper walletAddressMapper;

    public void run() {
        System.out.println("1");
        String weekStr = redisTemplate.opsForValue().get("weekly:lastWeekNo");
        int lastWeek = (weekStr == null ? 6 : Integer.parseInt(weekStr));
        List<String> wallets = walletAddressMapper.selectWalletByNoStat(lastWeek);
        for (String wallet : wallets) {
            String keyLast = String.format("opinion:weekly:%s:%d", wallet, lastWeek);
            if (redisTemplate.hasKey(keyLast)) {
                Map<Object, Object> lastData = redisTemplate.opsForHash().entries(keyLast);
                if (lastData.containsKey("deltaPoint")) {
                    BigDecimal lastPoint = new BigDecimal((String) lastData.getOrDefault("deltaPoint", "0"));
//                    userDetail.put("lastPoint", lastPoint);
//                    needFetch = false;
                }
            }
        }
    }
}
