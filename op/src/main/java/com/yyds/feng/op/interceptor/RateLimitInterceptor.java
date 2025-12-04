package com.yyds.feng.op.interceptor;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private static final String LIMIT_IPS_KEY = "ratelimit:ips";

    private boolean isIpLimited(String ip) {
        Boolean has = redisTemplate.opsForSet().isMember(LIMIT_IPS_KEY, ip);
        return has != null && has;
    }

    private boolean isAllowed(String ip) {
        String key = "rl:ip:" + ip;
        Long count = redisTemplate.opsForValue().increment(key);

        if (count == 1) {
            redisTemplate.expire(key, 60, TimeUnit.SECONDS); // 60秒窗口
        }

        return count <= 1;
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty()) {
            return ip.split(",")[0].trim();
        }
        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isEmpty()) {
            return ip;
        }
        return request.getRemoteAddr();
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        String ip = getClientIp(request);

        // ⭐ 仅对特定 IP 限流
        // ⭐ 从 Redis 判断是否需要限流
        if (!isIpLimited(ip)) {
            return true; // 不在限流 IP 列表中
        }

        if (!isAllowed(ip)) {
            response.setStatus(429); // Too Many Requests
            response.getWriter().write("哥别调我了，联系我把代码发你自己跑吧。wx：ldf_1126");
            return false;
        }

        return true;
    }
}
