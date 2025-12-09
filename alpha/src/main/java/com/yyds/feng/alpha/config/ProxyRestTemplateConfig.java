package com.yyds.feng.op.config;

import com.yyds.feng.common.entity.ProxyInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 代理 RestTemplate 配置 - 使用代理池轮询（预创建 HttpClient）
 */
@Slf4j
@Configuration
public class ProxyRestTemplateConfig {

        @Autowired
        private ProxyPool proxyPool;

        private final List<RestTemplate> restTemplates = new ArrayList<>();
        private final AtomicInteger index = new AtomicInteger(0);

        private static final int TIMEOUT = 5000;

        @PostConstruct
        public void init() {
                // 为每个代理预创建 RestTemplate
                for (int i = 0; i < proxyPool.size(); i++) {
                        ProxyInfo proxy = proxyPool.getNextProxy();
                        RestTemplate rt = createRestTemplateForProxy(proxy);
                        restTemplates.add(rt);
                }
                log.info("已为 {} 个代理创建 RestTemplate", restTemplates.size());
        }

        private RestTemplate createRestTemplateForProxy(ProxyInfo proxy) {
                // 代理认证
                CredentialsProvider credsProvider = new BasicCredentialsProvider();
                credsProvider.setCredentials(
                                new AuthScope(proxy.getHost(), proxy.getPort()),
                                new UsernamePasswordCredentials(proxy.getUsername(), proxy.getPassword()));

                HttpHost httpHost = new HttpHost(proxy.getHost(), proxy.getPort());

                // 连接池
                PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
                cm.setMaxTotal(200);
                cm.setDefaultMaxPerRoute(50);

                CloseableHttpClient httpClient = HttpClients.custom()
                                .setConnectionManager(cm)
                                .setDefaultCredentialsProvider(credsProvider)
                                .setProxy(httpHost)
                                .setKeepAliveStrategy((response, context) -> 30_000)
                                .build();

                HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);

                factory.setConnectTimeout(TIMEOUT);
                factory.setReadTimeout(TIMEOUT);
                factory.setConnectionRequestTimeout(TIMEOUT);

                return new RestTemplate(factory);
        }

        @Bean
        public RestTemplate restTemplate() {
                // 返回一个代理 RestTemplate，会轮询选择
                return new ProxyPoolRestTemplate(this);
        }

        /**
         * 获取下一个 RestTemplate（轮询）
         */
        public RestTemplate getNextRestTemplate() {
                if (restTemplates.isEmpty()) {
                        throw new IllegalStateException("No RestTemplates available");
                }
                int idx = Math.abs(index.getAndIncrement() % restTemplates.size());
                return restTemplates.get(idx);
        }

        /**
         * 代理 RestTemplate，每次请求委托给不同的实际 RestTemplate
         */
        public static class ProxyPoolRestTemplate extends RestTemplate {
                private final ProxyRestTemplateConfig config;

                public ProxyPoolRestTemplate(ProxyRestTemplateConfig config) {
                        this.config = config;
                }

                @Override
                public <T> T getForObject(String url, Class<T> responseType, Object... uriVariables) {
                        return config.getNextRestTemplate().getForObject(url, responseType, uriVariables);
                }

                @Override
                public <T> T getForObject(String url, Class<T> responseType, java.util.Map<String, ?> uriVariables) {
                        return config.getNextRestTemplate().getForObject(url, responseType, uriVariables);
                }

                @Override
                public <T> T getForObject(java.net.URI url, Class<T> responseType) {
                        return config.getNextRestTemplate().getForObject(url, responseType);
                }
        }
}