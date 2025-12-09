package com.yyds.feng.alpha.controller;

import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yyds.feng.alpha.entity.UserDefaultSource;
import com.yyds.feng.alpha.entity.UserScoreReport;
import com.yyds.feng.alpha.service.AirdropCacheService;
import com.yyds.feng.alpha.service.UserDefaultSourceService;
import com.yyds.feng.alpha.service.UserScoreReportService;
import com.yyds.feng.common.util.R;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping
public class ReportController {

    @Resource
    private UserScoreReportService reportService;


    @Resource
    private UserDefaultSourceService userDefaultSourceService;

    @Resource
    private AirdropCacheService airdropCacheService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping("/report")
    public R report(@RequestBody ReportRequest req) {

        UserScoreReport report = new UserScoreReport();
        report.setUsername(req.getUsername());
        Integer source = req.getSource();
        boolean isAirdrop = req.isAirdropTrue();
        if (isAirdrop) {
            Integer defaultSource = userDefaultSourceService.getDefaultSource(req.getUsername());
            source = (defaultSource != null ? defaultSource : 17) - 15;
        }
        report.setSource(source);
        report.setBalance(req.getBalance());
        report.setAirdrop(isAirdrop);
        // 透传前端指定的日期（如未传则在 service 中填充当天）
        report.setReportDate(req.getReportDate());

        reportService.saveReport(report);

        return R.ok();
    }

    @GetMapping("/getData")
    public R getData() {
        // 查询所有数据
        List<UserScoreReport> reports = reportService.getAllReports();

        // 查询用户自定义默认分数
        Map<String, Integer> defaultSourceMap = userDefaultSourceService.listAll()
                .stream()
                .filter(item -> item.getUsername() != null)
                .collect(Collectors.toMap(
                        item -> item.getUsername().trim(),
                        UserDefaultSource::getDefaultSource,
                        (existing, replacement) -> existing));

        // 获取所有唯一用户名
        Set<String> uniqueUsers = reports.stream()
                .map(UserScoreReport::getUsername)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        uniqueUsers.addAll(defaultSourceMap.keySet());

        // 计算过去15天的日期列表
        // 过去15天，不包含今天
        LocalDate today = LocalDate.now().minusDays(1);
        List<LocalDate> last15Days = new ArrayList<>();
        for (int i = 14; i >= 0; i--) {
            last15Days.add(today.minusDays(i));
        }

        // 创建一个映射来快速查找现有数据
        Map<String, UserScoreReport> reportMap = reports.stream()
                .collect(Collectors.toMap(
                        report -> report.getReportDate() + "_" + report.getUsername(),
                        report -> report,
                        (existing, replacement) -> existing));

        // 构造完整的数据集
        List<ReportData> result = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd");

        for (LocalDate date : last15Days) {
            String dateStr = date.format(formatter);
            ReportData reportData = new ReportData();
            reportData.setReportDate(dateStr);

            List<UserInfo> userInfoList = new ArrayList<>();
            for (String username : uniqueUsers) {
                Integer defaultSource = defaultSourceMap.getOrDefault(username, 17);
                String key = dateStr + "_" + username;
                UserScoreReport report = reportMap.get(key);

                UserInfo userInfo = new UserInfo();
                userInfo.setUser(username);

                if (report != null) {
                    // 使用实际数据
                    userInfo.setSource(report.getSource() != null ? report.getSource() : defaultSource);
                    userInfo.setBalance(report.getBalance() != null ? report.getBalance() : 0.0);
                } else {
                    // 补全默认数据
                    userInfo.setSource(defaultSource);
                    userInfo.setBalance(0.0);
                }

                userInfoList.add(userInfo);
            }

            reportData.setUserInfo(userInfoList);
            result.add(reportData);
        }

        return R.ok(result);
    }

    @PostMapping("/saveSource")
    public R saveSource(@RequestBody SourceRequest req) {
        if (req == null || req.getUsername() == null || req.getUsername().trim().isEmpty()) {
            return R.error("username不能为空");
        }
        if (req.getDefaultSource() == null) {
            return R.error("defaultSource不能为空");
        }

        UserDefaultSource source = new UserDefaultSource();
        source.setUsername(req.getUsername());
        source.setDefaultSource(req.getDefaultSource());

        userDefaultSourceService.saveDefaultSource(source);
        return R.ok();
    }

    @GetMapping("/getSource")
    public R getSource() {
        List<UserDefaultSource> sources = userDefaultSourceService.listAll();
        return R.ok(sources);
    }

    @GetMapping("/airdropList")
    public R airdropList() {
        List<String> reports = reportService.getTodayReportUsers();
        return R.ok(reports);
    }

    @GetMapping("/airdropList2")
    public String airdropList2() {
        String cached = airdropCacheService.getCachedAirdropData();
        if (cached == null) {
            cached = airdropCacheService.refreshCache();
        }
        if (cached == null) {
            return JSONObject.toJSONString("airdrop cache empty");
        }
        return JSONObject.toJSONString(cached);
    }


    @Data
    public static class ReportRequest {
        private String username;
        private Integer source;
        private Double balance;
        @Setter(AccessLevel.NONE)
        private Boolean airdrop;
        private String reportDate;

        @JsonSetter("airdrop")
        public void setAirdrop(Object airdrop) {
            this.airdrop = convertToBoolean(airdrop);
        }

        public boolean isAirdropTrue() {
            return Boolean.TRUE.equals(this.airdrop);
        }

        private Boolean convertToBoolean(Object value) {
            if (value == null) {
                return null;
            }
            if (value instanceof Boolean) {
                return (Boolean) value;
            }
            if (value instanceof Number) {
                return ((Number) value).intValue() == 1;
            }
            if (value instanceof CharSequence) {
                String str = value.toString().trim();
                if (str.isEmpty()) {
                    return null;
                }
                if ("1".equals(str)) {
                    return true;
                }
                if ("0".equals(str)) {
                    return false;
                }
                return Boolean.parseBoolean(str);
            }
            return null;
        }
    }

    @Data
    public static class ReportData {
        private String reportDate;
        private List<UserInfo> userInfo;
    }

    @Data
    public static class UserInfo {
        private String user;
        private Integer source;
        private Double balance;
    }

    @Data
    public static class SourceRequest {
        private String username;
        private Integer defaultSource;
    }
}
