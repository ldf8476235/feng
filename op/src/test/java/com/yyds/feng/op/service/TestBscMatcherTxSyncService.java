package com.yyds.feng.op.service;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@Slf4j
@SpringBootTest
@TestPropertySource(properties = {
        "bsc.sync-enabled=false",
        "bsc.ws-url="
})
public class TestBscMatcherTxSyncService {

    @Autowired
    private BscMatcherTxSyncService syncService;

    @Test
    public void testSyncRange() throws Exception {
        long fromBlock = 72325890L;
        long toBlock = 72725046L;
        log.info("Sync range: {} - {}", fromBlock, toBlock);
        syncService.syncRangeForTest(fromBlock, toBlock);
    }
}
