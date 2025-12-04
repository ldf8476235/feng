package com.yyds.feng.op.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigInteger;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class BscRpcClient {

    @Value("${bsc.rpc-url}")
    private String rpcUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    public BigInteger getLatestBlockNumber() {
        JSONObject body = new JSONObject();
        body.put("jsonrpc", "2.0");
        body.put("id", 1);
        body.put("method", "eth_blockNumber");
        body.put("params", Collections.emptyList());

        JSONObject resp = post(body);
        String hex = resp.getString("result");
        return new BigInteger(hex.substring(2), 16);
    }

    public List<JSONObject> getLogs(JSONObject filter) {
        JSONObject body = new JSONObject();
        body.put("jsonrpc", "2.0");
        body.put("id", 1);
        body.put("method", "eth_getLogs");
        body.put("params", Collections.singletonList(filter));

        JSONObject resp = post(body);
        return resp.getJSONArray("result").toJavaList(JSONObject.class);
    }

    private JSONObject post(JSONObject body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>(body.toJSONString(), headers);
        ResponseEntity<String> response = restTemplate.exchange(
                rpcUrl,
                HttpMethod.POST,
                entity,
                String.class
        );

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("RPC 调用失败: " + response.getStatusCodeValue());
        }

        JSONObject json = JSON.parseObject(response.getBody());
        if (json.containsKey("error")) {
            throw new RuntimeException("RPC 返回错误: " + json.getJSONObject("error"));
        }
        return json;
    }
}
