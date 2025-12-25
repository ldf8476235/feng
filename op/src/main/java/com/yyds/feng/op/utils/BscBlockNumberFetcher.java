package com.yyds.feng.op.utils;

import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterNumber;
import org.web3j.protocol.core.methods.response.EthBlock;
import org.web3j.protocol.core.methods.response.EthBlockNumber;
import org.web3j.protocol.http.HttpService;

import java.math.BigInteger;
import java.time.LocalDate;
import java.time.ZoneId;

public class BscBlockNumberFetcher {

    private static final String RPC_URL =
            "https://bsc-mainnet.nodereal.io/v1/e41e48d6f1d945c6bb400d521e4d9321";

    private static final ZoneId ZONE_ID = ZoneId.of("Asia/Shanghai");

    public static void main(String[] args) throws Exception {
        LocalDate date = resolveDate(args);
        long targetTs = date.atStartOfDay(ZONE_ID).toEpochSecond();

        Web3j web3j = Web3j.build(new HttpService(RPC_URL));
        try {
            EthBlockNumber latestResp = web3j.ethBlockNumber().send();
            BigInteger latest = latestResp.getBlockNumber();

            BigInteger block = findBlockByTimestamp(web3j, targetTs, latest);
            System.out.println("Date: " + date + " -> block: " + block);
        } finally {
            web3j.shutdown();
        }
    }

    private static LocalDate resolveDate(String[] args) {
        if (args != null && args.length > 0 && args[0] != null && !args[0].trim().isEmpty()) {
            String input = args[0].trim();
            if (input.length() == 5) {
                int year = LocalDate.now(ZONE_ID).getYear();
                int month = Integer.parseInt(input.substring(0, 2));
                int day = Integer.parseInt(input.substring(3, 5));
                return LocalDate.of(year, month, day);
            }
            return LocalDate.parse(input);
        }
        int year = LocalDate.now(ZONE_ID).getYear();
        return LocalDate.of(year, 12, 20);
    }

    private static BigInteger findBlockByTimestamp(Web3j web3j, long targetTs, BigInteger high)
            throws Exception {
        BigInteger low = BigInteger.ONE;

        while (low.compareTo(high) <= 0) {
            BigInteger mid = low.add(high).divide(BigInteger.valueOf(2));

            EthBlock block = web3j.ethGetBlockByNumber(
                    new DefaultBlockParameterNumber(mid),
                    false
            ).send();

            if (block.getBlock() == null) {
                return mid;
            }

            long ts = block.getBlock().getTimestamp().longValue();

            if (ts < targetTs) {
                low = mid.add(BigInteger.ONE);
            } else {
                high = mid.subtract(BigInteger.ONE);
            }
        }
        return low;
    }
}
