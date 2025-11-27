package com.yyds.feng.op.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@RestController
@RequestMapping("/test")
public class Test {

    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(8);

    @RequestMapping(value = "test")
    public String test() {
        System.out.println("1");
        return "1";
    }

    @PostMapping("/getData")
    public String getData(@RequestBody WalletRequest request) {
        List<String> wallets = request == null
                ? new ArrayList<>()
                : Objects.requireNonNullElse(request.getWallets(), new ArrayList<>());
        List<Future<?>> futures = new ArrayList<>();

        for (String wallet : wallets) {
            futures.add(EXECUTOR.submit(() -> fetchAndLogWallet(wallet)));
        }

        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (Exception e) {
                System.out.println("Failed to process wallet: " + e.getMessage());
            }
        }

        return "processed";
    }

    private void fetchAndLogWallet(String wallet) {
        try {
            String encodedWallet = URLEncoder.encode(wallet, StandardCharsets.UTF_8.name());
            URL url = new URL("https://proxy.opinion.trade:8443/api/bsc/api/v2/leaderboard/"
                    + encodedWallet + "?dataType=points&chainId=56");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);

            int status = connection.getResponseCode();
            InputStream inputStream = status >= 200 && status < 300
                    ? connection.getInputStream()
                    : connection.getErrorStream();
            String response = readResponse(inputStream);
            System.out.println("Wallet " + wallet + " response: " + response);
        } catch (IOException e) {
            System.out.println("Error fetching data for wallet " + wallet + ": " + e.getMessage());
        }
    }

    private String readResponse(InputStream inputStream) throws IOException {
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
        }
        return builder.toString();
    }

    public static class WalletRequest {
        private List<String> wallets;

        public List<String> getWallets() {
            return wallets;
        }

        public void setWallets(List<String> wallets) {
            this.wallets = wallets;
        }
    }
}
