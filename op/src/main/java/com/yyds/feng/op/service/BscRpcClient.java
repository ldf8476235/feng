package com.yyds.feng.op.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigInteger;
import java.util.ArrayList;
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

    public JSONObject getAssetTransfers(long startBlock,
                                        long endBlock,
                                        String pageKey,
                                        String contractAddress) {
        JSONObject params = new JSONObject();
        params.put("fromBlock", toHex(startBlock));
        params.put("toBlock", toHex(endBlock));
        if (contractAddress != null && !contractAddress.trim().isEmpty()) {
            params.put("contractAddresses", Collections.singletonList(contractAddress));
        }
        params.put("category", Collections.singletonList("20"));
        params.put("maxCount", "0x3e8");
        params.put("order", "asc");

        if (pageKey != null && !pageKey.trim().isEmpty()) {
            params.put("pageKey", pageKey);
        }

        JSONObject body = new JSONObject();
        body.put("jsonrpc", "2.0");
        body.put("id", 1);
        body.put("method", "nr_getAssetTransfers");
        body.put("params", Collections.singletonList(params));

        JSONObject resp = post(body);
        JSONObject result = resp.getJSONObject("result");
        return result == null ? new JSONObject() : result;
    }

    public Long getBlockTimestamp(long blockNumber) {
        List<Object> params = new ArrayList<>();
        params.add(toHex(blockNumber));
        params.add(Boolean.FALSE);

        JSONObject body = new JSONObject();
        body.put("jsonrpc", "2.0");
        body.put("id", 1);
        body.put("method", "eth_getBlockByNumber");
        body.put("params", params);

        JSONObject resp = post(body);
        JSONObject result = resp.getJSONObject("result");
        if (result == null) {
            return null;
        }
        String hex = result.getString("timestamp");
        if (hex == null || hex.isEmpty()) {
            return null;
        }
        return parseHexLong(hex);
    }

    private String toHex(long value) {
        return "0x" + Long.toHexString(value);
    }

    private Long parseHexLong(String hex) {
        String clean = hex.startsWith("0x") || hex.startsWith("0X")
                ? hex.substring(2)
                : hex;
        if (clean.isEmpty()) {
            return null;
        }
        return new BigInteger(clean, 16).longValue();
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
