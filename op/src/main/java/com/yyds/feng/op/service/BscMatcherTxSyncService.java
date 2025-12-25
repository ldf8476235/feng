package com.yyds.feng.op.service;

import com.yyds.feng.common.entity.BscMatcherTx;
import com.yyds.feng.op.mapper.BscMatcherTxMapper;
import io.reactivex.disposables.Disposable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.web3j.crypto.Hash;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.methods.response.EthBlock;
import org.web3j.protocol.core.methods.response.EthBlockNumber;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.Transaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;
import org.web3j.protocol.websocket.WebSocketService;
import org.web3j.utils.Numeric;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.net.ConnectException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BscMatcherTxSyncService {
    private static final String REDIS_LAST_BLOCK_KEY = "bsc:sync:last_block";
    private static final String TRANSFER_TOPIC = Hash.sha3String("Transfer(address,address,uint256)");
    private static final ZoneId BLOCK_TIME_ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMdd");
    private static final LocalTime DAY_CUTOFF = LocalTime.of(8, 0);
    private static final String SIDE_BUY = "BUY";
    private static final String SIDE_SELL = "SELL";
    private static final int INSERT_BATCH_SIZE = 50;

    private final BscMatcherTxMapper txMapper;
    private final StringRedisTemplate redisTemplate;

    @Value("${bsc.rpc-url}")
    private String rpcUrl;

    @Value("${bsc.ws-url}")
    private String wsUrl;

    @Value("${bsc.start-block:0}")
    private long startBlock;

    private String matcherAddress = "0x5F45344126D6488025B0b84A3A8189F2487a7246";

    @Value("${bsc.token-contract}")
    private String tokenContract;

    @Value("${bsc.token-decimals:18}")
    private int tokenDecimals;

    @Value("${bsc.batch-size:2000}")
    private int batchSize;

    @Value("${bsc.sync-enabled:true}")
    private boolean syncEnabled;

    private final ExecutorService syncExecutor = Executors.newSingleThreadExecutor();
    private Web3j httpWeb3j;
    private Web3j wsWeb3j;
    private WebSocketService wsService;
    private Disposable newHeadSubscription;

    @PostConstruct
    public void start() {
        if (!syncEnabled) {
            log.info("BSC sync disabled by config.");
            return;
        }
        syncExecutor.submit(this::runSync);
    }

    void syncRangeForTest(long from, long to) throws IOException {
        if (httpWeb3j == null) {
            httpWeb3j = Web3j.build(new HttpService(rpcUrl));
        }
        syncRange(from, to);
    }

    @PreDestroy
    public void shutdown() {
        if (newHeadSubscription != null && !newHeadSubscription.isDisposed()) {
            newHeadSubscription.dispose();
        }
        if (wsService != null) {
            wsService.close();
        }
        if (wsWeb3j != null) {
            wsWeb3j.shutdown();
        }
        if (httpWeb3j != null) {
            httpWeb3j.shutdown();
        }
        syncExecutor.shutdownNow();
    }

    private void runSync() {
        try {
            httpWeb3j = Web3j.build(new HttpService(rpcUrl));
            syncHistory();
            startWebSocket();
        } catch (Exception e) {
            log.error("BSC sync init failed: {}", e.getMessage(), e);
        }
    }

    private void syncHistory() throws IOException {
        long fromBlock = resolveStartBlock();
        long latestBlock = getLatestBlockNumber();

        if (fromBlock > latestBlock) {
            log.info("History sync skipped: fromBlock={} latestBlock={}", fromBlock, latestBlock);
            return;
        }

        int effectiveBatchSize = Math.max(batchSize, 1);
        log.info("History sync start: fromBlock={} latestBlock={} batchSize={}", fromBlock, latestBlock, effectiveBatchSize);

        for (long start = fromBlock; start <= latestBlock; start += effectiveBatchSize) {
            long end = Math.min(start + effectiveBatchSize - 1, latestBlock);
            try {
                syncRange(start, end);
                saveLastBlock(end);
                log.info("History sync progress: {} - {}", start, end);
            } catch (Exception e) {
                log.error("History sync failed at {} - {}: {}", start, end, e.getMessage(), e);
                break;
            }
        }
    }

    private void startWebSocket() {
        if (wsUrl == null || wsUrl.trim().isEmpty()) {
            log.warn("WS url is empty, skip WS subscription.");
            return;
        }
        try {
            wsService = new WebSocketService(wsUrl, false);
            wsService.connect();
            wsWeb3j = Web3j.build(wsService);
        } catch (ConnectException e) {
            log.error("WS connect failed: {}", e.getMessage(), e);
            return;
        }

        newHeadSubscription = wsWeb3j.newHeadsNotifications().subscribe(
                head -> {
                    String numberHex = head.getParams() != null && head.getParams().getResult() != null
                            ? head.getParams().getResult().getNumber() : null;
                    if (numberHex == null) {
                        return;
                    }
                    long blockNumber = Numeric.toBigInt(numberHex).longValue();
                    try {
                        syncRange(blockNumber, blockNumber);
                        saveLastBlock(blockNumber);
                    } catch (Exception e) {
                        log.error("WS block sync failed: {}", e.getMessage(), e);
                    }
                },
                error -> log.error("WS subscription error: {}", error.getMessage(), error)
        );

        log.info("WS subscription started.");
    }

    private void syncRange(long from, long to) throws IOException {
        String matcherAddr = normalizeAddress(matcherAddress);
        log.info("Sync range: from={} to={}", from, to);
        if (matcherAddr == null) {
            log.warn("Matcher address is empty, skip sync.");
            return;
        }

        List<BscMatcherTx> batch = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        int matchedTxCount = 0;
        int matchedLogCount = 0;
        int totalTransferLogCount = 0;

        for (long blockNumber = from; blockNumber <= to; blockNumber++) {
            EthBlock ethBlock = httpWeb3j.ethGetBlockByNumber(
                    DefaultBlockParameter.valueOf(BigInteger.valueOf(blockNumber)), true
            ).send();
            EthBlock.Block block = ethBlock.getBlock();
            if (block == null) {
                continue;
            }

            LocalDateTime blockTime = toBlockTime(block.getTimestamp());
            List<EthBlock.TransactionResult> transactions = block.getTransactions();
            if (transactions == null || transactions.isEmpty()) {
                continue;
            }

            for (EthBlock.TransactionResult<?> txResult : transactions) {
                Object txObject = txResult.get();
                if (!(txObject instanceof Transaction)) {
                    continue;
                }
                Transaction tx = (Transaction) txObject;
                if (!isProjectTx(tx, matcherAddr)) {
                    continue;
                }
                boolean txMatched = false;

                Optional<TransactionReceipt> receiptOpt = httpWeb3j.ethGetTransactionReceipt(tx.getHash())
                        .send()
                        .getTransactionReceipt();
                if (!receiptOpt.isPresent()) {
                    continue;
                }

                List<Log> receiptLogs = receiptOpt.get().getLogs();
                if (receiptLogs == null || receiptLogs.isEmpty()) {
                    continue;
                }

                List<BscMatcherTx> transferRecords =
                        extractTransferRecords(receiptLogs, blockNumber, blockTime);
                totalTransferLogCount += transferRecords.size();

                List<BscMatcherTx> matcherRecords = new ArrayList<>();
                for (BscMatcherTx record : transferRecords) {
                    if (!matcherAddr.equals(record.getFromAddress())
                            && !matcherAddr.equals(record.getToAddress())) {
                        continue;
                    }
                    matcherRecords.add(record);
                }

                List<BscMatcherTx> tradeRecords = buildTradeRecords(matcherRecords, matcherAddr);
                for (BscMatcherTx record : tradeRecords) {
                    String uniqueKey = record.getTxHash() + ":" + record.getAddress() + ":" + record.getSide();
                    if (!seen.add(uniqueKey)) {
                        continue;
                    }

                    batch.add(record);
                    matchedLogCount++;
                    txMatched = true;

                    if (batch.size() >= INSERT_BATCH_SIZE) {
                        txMapper.insertBatchIgnore(batch);
                        batch.clear();
                    }
                }
                if (txMatched) {
                    matchedTxCount++;
                }
                if (!batch.isEmpty()) {
                    txMapper.insertBatchIgnore(batch);
                }
            }
        }
        log.info("Transfer logs total: {}, matched tx: {}, matched logs: {} ({} - {})",
                totalTransferLogCount, matchedTxCount, matchedLogCount, from, to);
    }

    private List<BscMatcherTx> extractTransferRecords(List<Log> receiptLogs,
                                                      long blockNumber,
                                                      LocalDateTime blockTime) {
        List<BscMatcherTx> records = new ArrayList<>();
        if (receiptLogs == null || receiptLogs.isEmpty()) {
            return records;
        }
        for (Log receiptLog : receiptLogs) {
            List<String> topics = receiptLog.getTopics();
            if (topics == null || topics.size() != 3) {
                continue;
            }
            if (!TRANSFER_TOPIC.equalsIgnoreCase(topics.get(0))) {
                continue;
            }
            if (receiptLog.getData() == null || "0x".equals(receiptLog.getData())) {
                continue;
            }

            String fromAddress = topicToAddress(topics.get(1));
            String toAddress = topicToAddress(topics.get(2));
            if (fromAddress == null || toAddress == null) {
                continue;
            }

            BscMatcherTx record = toMatcherTx(receiptLog, blockNumber, blockTime,
                    fromAddress, toAddress);
            if (record == null) {
                continue;
            }
            records.add(record);
        }
        return records;
    }

    private BscMatcherTx toMatcherTx(Log logItem, long blockNumber, LocalDateTime blockTime, String from, String to) {
        List<String> topics = logItem.getTopics();
        if (topics == null || topics.size() < 3) {
            return null;
        }

        BigInteger rawValue = Numeric.toBigInt(logItem.getData());
        BigDecimal amount = new BigDecimal(rawValue).movePointLeft(tokenDecimals);

        BscMatcherTx record = new BscMatcherTx();
        record.setTxHash(logItem.getTransactionHash());
        record.setBlockNumber(blockNumber);
        record.setBlockTime(blockTime);
        record.setFromAddress(from);
        record.setToAddress(to);
        record.setValueRaw(rawValue.toString());
        record.setValueAmount(amount);
        record.setLogIndex(logItem.getLogIndex() != null ? logItem.getLogIndex().intValue() : 0);
        return record;
    }

    private String formatBlockDate(LocalDateTime blockTime) {
        LocalDateTime effectiveTime = blockTime != null ? blockTime : LocalDateTime.now(BLOCK_TIME_ZONE);
        LocalDate date = effectiveTime.toLocalDate();
        if (!effectiveTime.toLocalTime().isBefore(DAY_CUTOFF)) {
            date = date.plusDays(1);
        }
        return DATE_FORMATTER.format(date);
    }

    private List<BscMatcherTx> buildTradeRecords(List<BscMatcherTx> records, String matcherAddr) {
        List<BscMatcherTx> result = new ArrayList<>();
        if (records == null || records.isEmpty()) {
            return result;
        }

        Map<String, BigDecimal> buyerTotals = new HashMap<>();
        Map<String, BigDecimal> sellerTotals = new HashMap<>();
        BscMatcherTx base = null;
        for (BscMatcherTx record : records) {
            if (base == null) {
                base = record;
            }
            BigDecimal amount = record.getValueAmount();
            if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }
            if (matcherAddr.equals(record.getToAddress())) {
                String buyer = record.getFromAddress();
                if (buyer != null && !matcherAddr.equals(buyer)) {
                    buyerTotals.merge(buyer, amount, BigDecimal::add);
                }
            } else if (matcherAddr.equals(record.getFromAddress())) {
                String seller = record.getToAddress();
                if (seller != null && !matcherAddr.equals(seller)) {
                    sellerTotals.merge(seller, amount, BigDecimal::add);
                }
            }
        }

        if (base == null) {
            return result;
        }

        String date = formatBlockDate(base.getBlockTime());
        for (Map.Entry<String, BigDecimal> entry : buyerTotals.entrySet()) {
            BigDecimal amount = entry.getValue().setScale(2, RoundingMode.HALF_UP);
            if (amount.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }
            result.add(buildAggregatedRecord(base, date, entry.getKey(), amount, SIDE_BUY));
        }

        Map.Entry<String, BigDecimal> maxSeller = null;
        for (Map.Entry<String, BigDecimal> entry : sellerTotals.entrySet()) {
            BigDecimal amount = entry.getValue();
            if (amount == null) {
                continue;
            }
            if (maxSeller == null || amount.compareTo(maxSeller.getValue()) > 0) {
                maxSeller = entry;
            }
        }
        if (maxSeller != null) {
            BigDecimal amount = maxSeller.getValue().setScale(2, RoundingMode.HALF_UP);
            if (amount.compareTo(BigDecimal.ZERO) != 0) {
                result.add(buildAggregatedRecord(base, date, maxSeller.getKey(), amount, SIDE_SELL));
            }
        }
        return result;
    }

    private BscMatcherTx buildAggregatedRecord(BscMatcherTx base,
                                               String date,
                                               String address,
                                               BigDecimal amount,
                                               String side) {
        BscMatcherTx aggregated = new BscMatcherTx();
        aggregated.setTxHash(base.getTxHash());
        aggregated.setBlockNumber(base.getBlockNumber());
        aggregated.setBlockTime(base.getBlockTime());
        aggregated.setDate(date);
        aggregated.setAddress(address);
        aggregated.setSide(side);
        aggregated.setValueAmount(amount);
        return aggregated;
    }

    private boolean isProjectTx(Transaction tx, String matcherAddr) {
        if (tx == null || matcherAddr == null || matcherAddr.trim().isEmpty()) {
            return false;
        }
        String input = tx.getInput();
        if (input == null || input.trim().isEmpty() || "0x".equalsIgnoreCase(input.trim())) {
            return false;
        }
        String normalizedInput = input.toLowerCase();
        String normalizedAddr = matcherAddr.toLowerCase();
        String addrNoPrefix = Numeric.cleanHexPrefix(normalizedAddr);
        return normalizedInput.contains(addrNoPrefix) || normalizedInput.contains(normalizedAddr);
    }


    private long resolveStartBlock() {
        String lastBlockStr = redisTemplate.opsForValue().get(REDIS_LAST_BLOCK_KEY);
        if (lastBlockStr == null || lastBlockStr.trim().isEmpty()) {
            return startBlock;
        }
        try {
            long lastBlock = Long.parseLong(lastBlockStr);
            return Math.max(startBlock, lastBlock + 1);
        } catch (NumberFormatException e) {
            log.warn("Invalid redis last block value: {}", lastBlockStr);
            return startBlock;
        }
    }

    private void saveLastBlock(long blockNumber) {
        redisTemplate.opsForValue().set(REDIS_LAST_BLOCK_KEY, String.valueOf(blockNumber));
    }

    private long getLatestBlockNumber() throws IOException {
        EthBlockNumber blockNumber = httpWeb3j.ethBlockNumber().send();
        return blockNumber.getBlockNumber().longValue();
    }

    private String normalizeAddress(String address) {
        return address.toLowerCase();
    }

    private String topicToAddress(String topic) {
        if (topic == null) {
            return null;
        }
        String clean = Numeric.cleanHexPrefix(topic);
        if (clean.length() < 40) {
            return "0x" + clean.toLowerCase();
        }
        return "0x" + clean.substring(clean.length() - 40).toLowerCase();
    }

    private LocalDateTime toBlockTime(BigInteger timestamp) {
        if (timestamp == null) {
            return LocalDateTime.now(BLOCK_TIME_ZONE);
        }
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(timestamp.longValue()), BLOCK_TIME_ZONE);
    }
}
