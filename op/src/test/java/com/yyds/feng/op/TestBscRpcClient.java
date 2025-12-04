package com.yyds.feng.op;

import com.alibaba.fastjson.JSONObject;
import com.yyds.feng.op.service.BscRpcClient;
import com.yyds.feng.op.task.SaveRedisToMysql;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.websocket.WebSocketClient;
import org.web3j.protocol.websocket.WebSocketService;

import java.math.BigInteger;
import java.net.ConnectException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;

@Slf4j
@SpringBootTest
public class TestBscRpcClient {

    @Autowired
    private BscRpcClient bscRpcClient;

    @Autowired
    SaveRedisToMysql saveRedisToMysql;
    /**
     * 测试获取最新区块高度
     */
    @Test
    public void testGetLatestBlock() {
        BigInteger block = bscRpcClient.getLatestBlockNumber();
        log.info("最新区块高度: {}", block);
    }

    /**
     * 测试 getLogs
     * 查询一个简单的 Transfer 事件，看是否能返回结果
     *
     * 这里使用 BUSD Transfer topic0
     */
    @Test
    public void testGetLogs() {
        String contract = "0x55d398326f99059fF775485246999027B3197955"; // USDT(BUSD-T)
        String topicTransfer = "0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef";

        JSONObject filter = new JSONObject();
        filter.put("address", contract);
        filter.put("fromBlock", "0x" + Integer.toHexString(33000000)); // 任意较新的区块
        filter.put("toBlock", "latest");
        filter.put("topics", Collections.singletonList(topicTransfer));

        List<JSONObject> logs = bscRpcClient.getLogs(filter);

        log.info("查询到的日志数量: {}", logs.size());
        if (!logs.isEmpty()) {
            log.info("第一条日志信息: {}", logs.get(0).toJSONString());
        }
    }

    @Test
    public void testGetBlockByHash() throws URISyntaxException, ConnectException {
        WebSocketClient client = new WebSocketClient(new URI("wss://bsc.publicnode.com"));
        WebSocketService ws = new WebSocketService(client, false);
        ws.connect();

        Web3j web3j = Web3j.build(ws);

        web3j.web3ClientVersion().sendAsync().thenAccept(resp ->
                System.out.println("WS OK: " + resp.getWeb3ClientVersion())
        );

    }

    @Test
    public void saveRedisToMysql() throws URISyntaxException, ConnectException {
        saveRedisToMysql.run();
    }
}
