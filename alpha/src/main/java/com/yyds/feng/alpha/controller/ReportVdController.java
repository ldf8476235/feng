package com.yyds.feng.alpha.controller;

import com.yyds.feng.alpha.entity.UserDefaultSourceVd;
import com.yyds.feng.alpha.entity.UserScoreReportVd;
import com.yyds.feng.alpha.service.UserDefaultSourceVdService;
import com.yyds.feng.alpha.service.UserScoreReportVdService;
import com.yyds.feng.alpha.service.dto.UserAirdropInfo;
import com.yyds.feng.common.util.R;
import lombok.Data;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping
public class ReportVdController {

    @Resource
    private UserScoreReportVdService reportService;

    @Resource
    private UserDefaultSourceVdService userDefaultSourceService;

    @PostMapping("/vdreport")
    public R report(@RequestBody ReportRequest req) {

        UserScoreReportVd report = new UserScoreReportVd();
        report.setUsername(req.getUsername());
        Integer source = req.getSource();
        if (null == req.getAirdrop()) req.setAirdrop(0);
        if (null != req.getAirdrop() && req.getAirdrop() == 1) {
            Integer defaultSource = userDefaultSourceService.getDefaultSource(req.getUsername());
            source = (defaultSource != null ? defaultSource : 17) - 15;
        }
        report.setSource(source);
        report.setBalance(req.getBalance());
        report.setAirdrop(req.getAirdrop());
        // Pass through report date (service fills today if missing).
        report.setReportDate(req.getReportDate());

        reportService.saveReport(report);

        return R.ok();
    }

    @GetMapping("/vdgetData")
    public R getData() {
        // Query all data.
        List<UserScoreReportVd> reports = reportService.getAllReports();

        // Query user default sources.
        Map<String, Integer> defaultSourceMap = userDefaultSourceService.listAll()
                .stream()
                .filter(item -> item.getUsername() != null)
                .collect(Collectors.toMap(
                        item -> item.getUsername().trim(),
                        UserDefaultSourceVd::getDefaultSource,
                        (existing, replacement) -> existing));

        // Get all unique usernames.
        Set<String> uniqueUsers = reports.stream()
                .map(UserScoreReportVd::getUsername)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        uniqueUsers.addAll(defaultSourceMap.keySet());

        // Last 15 days, excluding today.
        LocalDate today = LocalDate.now().minusDays(1);
        List<LocalDate> last15Days = new ArrayList<>();
        for (int i = 14; i >= 0; i--) {
            last15Days.add(today.minusDays(i));
        }

        // Build lookup map for existing reports.
        Map<String, UserScoreReportVd> reportMap = reports.stream()
                .collect(Collectors.toMap(
                        report -> report.getReportDate() + "_" + report.getUsername(),
                        report -> report,
                        (existing, replacement) -> existing));

        // Build full data set.
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
                UserScoreReportVd report = reportMap.get(key);

                UserInfo userInfo = new UserInfo();
                userInfo.setUser(username);

                if (report != null) {
                    // Use actual data.
                    userInfo.setSource(report.getSource() != null ? report.getSource() : defaultSource);
                    userInfo.setBalance(report.getBalance() != null ? report.getBalance() : 0.0);
                } else {
                    // Fill with defaults.
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

    @PostMapping("/vdsaveSource")
    public R saveSource(@RequestBody SourceRequest req) {
        if (req == null || req.getUsername() == null || req.getUsername().trim().isEmpty()) {
            return R.error("username不能为空");
        }
        if (req.getDefaultSource() == null) {
            return R.error("defaultSource不能为空");
        }

        UserDefaultSourceVd source = new UserDefaultSourceVd();
        source.setUsername(req.getUsername());
        source.setDefaultSource(req.getDefaultSource());

        userDefaultSourceService.saveDefaultSource(source);
        return R.ok();
    }

    @GetMapping("/vdgetSource")
    public R getSource() {
        List<UserDefaultSourceVd> sources = userDefaultSourceService.listAll();
        return R.ok(sources);
    }

    @GetMapping("/vdairdropList")
    public R airdropList() {
        List<UserAirdropInfo> reports = reportService.getTodayAirdropUsers();
        return R.ok(reports);
    }

    @Data
    public static class ReportRequest {
        private String username;
        private Integer source;
        private Double balance;
        private Integer airdrop;
        private String reportDate;
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
