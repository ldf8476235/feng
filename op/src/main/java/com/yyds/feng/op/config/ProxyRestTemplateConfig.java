package com.yyds.feng.op.config;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class ProxyRestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {

        int timeout = 5000;

        // 代理认证
        CredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(
                new AuthScope("82.22.69.208", 7415),
                new UsernamePasswordCredentials("hgbgvnnv", "2xqc2ff091er")
        );

        HttpHost proxy = new HttpHost("82.22.69.208", 7415);

        // 连接池
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        cm.setMaxTotal(200);       // 总连接数
        cm.setDefaultMaxPerRoute(50); // 每路由最大连接数

        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(cm)
                .setDefaultCredentialsProvider(credsProvider)
                .setProxy(proxy)
                .setKeepAliveStrategy((response, context) -> 30_000)  // 30秒 keep-alive
                .build();

        HttpComponentsClientHttpRequestFactory factory =
                new HttpComponentsClientHttpRequestFactory(httpClient);

        factory.setConnectTimeout(timeout);
        factory.setReadTimeout(timeout);
        factory.setConnectionRequestTimeout(timeout);

        return new RestTemplate(factory);
    }
}