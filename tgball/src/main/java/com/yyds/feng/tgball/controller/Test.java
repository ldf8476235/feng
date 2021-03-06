package com.yyds.feng.tgball.controller;

import com.yyds.feng.common.util.*;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.concurrent.TimeUnit;

@RestController
@Slf4j
public class Test {

    @Autowired
    RedisUtils redisUtils;

    public static void main(String[] args) throws UnsupportedEncodingException {
        WxPush.push("TG","交易结束",WxPush.DEFAULT_KEY);
                ;
//        sendSms();
//        login();
//        my();
    }
    public static void sendSms(){
        String url="https://m1.zvip111.co/ac_sms.php";
        X509TrustManager manager = SSLSocketClientUtil.getX509TrustManager();
        OkHttpClient okHttpClient = new OkHttpClient
                .Builder()
                .sslSocketFactory(SSLSocketClientUtil.getSocketFactory(manager), manager)
                .hostnameVerifier(SSLSocketClientUtil.getHostnameVerifier())
                .followRedirects(false)
                .followSslRedirects(false)
                .build();
        RequestBody requestBody = RequestBody.create(MediaType.parse("application/x-www-form-urlencoded"), "step=checkSMS&func=login&smscode=6097&lastFour=2984&username=ASW205");
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Content-type", "application/x-www-form-urlencoded; charset=UTF-8")
                .addHeader("Host", "m1.zvip111.co")
                .addHeader("Accept", "*/*")
                .addHeader("Accept-Language", "zh-CN,zh-Hans;q=0.9")
                //.addHeader("Accept-Encoding", "gzip, deflate")
                .addHeader("Origin", "https://m1.zvip111.co")
                .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 7.1.2; SM-G977N Build/LMY48Z; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/92.0.4515.131 Mobile Safari/537.36")
                .addHeader("Connection", "keep-alive")
                .addHeader("Referer", "https://m1.zvip111.co/login.php")
                .addHeader("deviceInfo", "Mozilla/5.0 (Linux; Android 7.1.2; SM-G977N Build/LMY48Z; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/92.0.4515.131 Mobile Safari/537.36")
                .addHeader("Cookie", "loginInfo_cookie=eyJhY2NvdW50IjoiQVNXMjA1IiwicHdkIjoiV3NsZGYxMjM0NTYifQ%3D%3D; say=tg332wyaya120.245.114.126; PHPSESSID=3bl5e7gj5dga8dlbcs99j5oft6")
                .post(requestBody)
                .build();
        Call call = okHttpClient.newCall(request);
        try {
            Response response = call.execute();
            String result = response.body().string();//得到数据

            System.out.println(result);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static void my(){
        Connection.Response res = null;
        try {
            res = Jsoup.connect("https://m1.zvip111.co/my.php")
                    .method(Connection.Method.GET)
                    .timeout(10000)
                    .header("Cookie","popupshow=saw; PHPSESSID=3bl5e7gj5dga8dlbcs99j5oft6; say=tg332wyaya120.245.114.126; loginInfo_cookie=eyJhY2NvdW50IjoiQVNXMjA1IiwicHdkIjoiV3NsZGYxMjM0NTYifQ%3D%3D")
                    .validateTLSCertificates(false)
                    .execute();
            Document doc = res.parse();
//            System.out.println(doc);
            Element element = doc.getElementsByClass("type1 carousel-item").get(1);
            int num = Integer.parseInt(element.child(1).text());
            if (num == 0) {
                WxPush.push("TG","交易结束",WxPush.DEFAULT_KEY);
            }
            log.info("进行中订单:->{}",num);
            System.out.println(DateUtils.getSysTime() + "进行中订单:" + num);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public static void login() throws UnsupportedEncodingException {
        String url="https://m1.zvip111.co/ac_login.php";
        X509TrustManager manager = SSLSocketClientUtil.getX509TrustManager();
        OkHttpClient okHttpClient = new OkHttpClient
                .Builder()
                .sslSocketFactory(SSLSocketClientUtil.getSocketFactory(manager), manager)
                .hostnameVerifier(SSLSocketClientUtil.getHostnameVerifier())
                .followRedirects(false)
                .followSslRedirects(false)
                .build();
        RequestBody requestBody = RequestBody.create(MediaType.parse("application/x-www-form-urlencoded"), "lastFour=2984&account=ASW205&pwd=Wsldf123456&deviceInfo=Mozilla%2F5.0+(Linux%3B+Android+7.1.2%3B+SM-G977N+Build%2FLMY48Z%3B+wv)+AppleWebKit%2F537.36+(KHTML%2C+like+Gecko)+Version%2F4.0+Chrome%2F92.0.4515.131+Mobile+Safari%2F537.36&vga=Adreno+(TM)+640");
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Content-type", "application/x-www-form-urlencoded; charset=UTF-8")
                .addHeader("Host", "m1.zvip111.co")
                .addHeader("Accept", "*/*")
                .addHeader("Accept-Language", "zh-CN,zh-Hans;q=0.9")
                //.addHeader("Accept-Encoding", "gzip, deflate")
                .addHeader("Origin", "https://m1.zvip111.co")
                .addHeader("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 15_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/15.5 Mobile/15E148 Safari/604.1")
                .addHeader("Connection", "keep-alive")
                .addHeader("Referer", "https://m1.zvip111.co/login.php")
                .addHeader("deviceInfo", "Mozilla/5.0 (iPhone; CPU iPhone OS 15_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/15.5 Mobile/15E148 Safari/604.1")
                .addHeader("Cookie", "loginInfo_cookie=eyJhY2NvdW50IjoiQVNXMjA1IiwicHdkIjoiV3NsZGYxMjM0NTYifQ%3D%3D; say=tg332wyaya106.3.197.194; PHPSESSID=3bl5e7gj5dga8dlbcs99j5oft6")
                .post(requestBody)
                .build();
        Call call = okHttpClient.newCall(request);
        try {
            Response response = call.execute();
            String result = response.body().string();//得到数据
            log.info(result);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @PostMapping("/test")
    void test(){
        try {
            Connection.Response res = Jsoup.connect("https://m1.zvip111.co/ac_loginData.php?")
                    .data("data", "{\"account\":\"ASW205\",\"pwd\":\"Wsldf123456\"}", "step", "hash")
                    .method(Connection.Method.POST)
                    .timeout(10000)
                    .validateTLSCertificates(false)
                    .execute();
//            Document doc = res.parse();
//            System.out.println(res.body());
//            System.out.println(doc);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
