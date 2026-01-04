package com.yyds.feng.alpha.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yyds.feng.alpha.entity.UserScoreReportVd;
import com.yyds.feng.alpha.mapper.UserScoreReportVdMapper;
import com.yyds.feng.alpha.service.UserScoreReportVdService;
import com.yyds.feng.alpha.service.dto.UserAirdropInfo;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class UserScoreReportVdServiceImpl implements UserScoreReportVdService {

    @Resource
    private UserScoreReportVdMapper reportMapper;

    @Override
    public void saveReport(UserScoreReportVd report) {

        boolean isAirdropRequest = report.getAirdrop() == 1 ? true : false;

        // Auto-fill today's date in MM-DD.
        String targetDate;
        if (report.getReportDate() == null || report.getReportDate().trim().isEmpty()) {
            targetDate = LocalDate.now().format(DateTimeFormatter.ofPattern("MM-dd"));
        } else {
            targetDate = report.getReportDate().trim();
        }
        report.setReportDate(targetDate);

        // Check if record exists for today.
        QueryWrapper<UserScoreReportVd> qw = new QueryWrapper<>();
        qw.eq("username", report.getUsername());
        qw.eq("report_date", report.getReportDate());

        UserScoreReportVd exist = reportMapper.selectOne(qw);

        if (exist != null) {
            if (isAirdropRequest) {
                // Existing record: update airdrop count and overwrite other fields.
                int existingCount = exist.getAirdropCount() == null ? 0 : exist.getAirdropCount();
                int updatedCount = isAirdropRequest ? existingCount + 1 : existingCount;

                report.setAirdropCount(updatedCount);
                // Keep airdrop status consistent with count.
                report.setAirdrop(report.getAirdrop());
                report.setId(exist.getId());
                report.setSource(exist.getSource() - 15);
                reportMapper.updateById(report);
            } else {
                report.setId(exist.getId());
                report.setSource(report.getSource());
                reportMapper.updateById(report);
            }
        } else {
            // No record: insert.
            int initialCount = isAirdropRequest ? 1 : 0;
            report.setAirdropCount(initialCount);
            report.setAirdrop(report.getAirdrop());
            reportMapper.insert(report);
        }
    }

    @Override
    public List<UserScoreReportVd> getAllReports() {
        return reportMapper.selectList(null);
    }

    @Override
    public List<UserAirdropInfo> getTodayAirdropUsers() {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("MM-dd"));

        QueryWrapper<UserScoreReportVd> wrapper = new QueryWrapper<>();
        wrapper.eq("report_date", today);
        wrapper.gt("airdrop_count", 0);

        List<UserScoreReportVd> reports = reportMapper.selectList(wrapper);
        return reports.stream()
                .filter(Objects::nonNull)
                .map(report -> {
                    UserAirdropInfo info = new UserAirdropInfo();
                    info.setUsername(report.getUsername());
                    info.setAirdropCount(report.getAirdropCount() == null ? 0 : report.getAirdropCount());
                    return info;
                })
                .filter(info -> info.getUsername() != null && !info.getUsername().trim().isEmpty())
                .peek(info -> info.setUsername(info.getUsername().trim()))
                .collect(Collectors.toList());
    }
}
