package com.yyds.feng.alpha.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yyds.feng.alpha.entity.UserScoreReport;
import com.yyds.feng.alpha.mapper.UserScoreReportMapper;
import com.yyds.feng.alpha.service.UserScoreReportService;
import com.yyds.feng.alpha.service.dto.UserAirdropInfo;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class UserScoreReportServiceImpl implements UserScoreReportService {

    @Resource
    private UserScoreReportMapper reportMapper;

    @Override
    public void saveReport(UserScoreReport report) {

        boolean isAirdropRequest = Boolean.TRUE.equals(report.getAirdrop());

        // 自动补充当天日期 MM-DD
        String targetDate;
        if (report.getReportDate() == null || report.getReportDate().trim().isEmpty()) {
            targetDate = LocalDate.now().format(DateTimeFormatter.ofPattern("MM-dd"));
        } else {
            targetDate = report.getReportDate().trim();
        }
        report.setReportDate(targetDate);

        // 检查当天是否已经存在
        QueryWrapper<UserScoreReport> qw = new QueryWrapper<>();
        qw.eq("username", report.getUsername());
        qw.eq("report_date", report.getReportDate());

        UserScoreReport exist = reportMapper.selectOne(qw);

        if (exist != null) {
            // 已存在 → 更新空投次数并覆盖其他字段
            int existingCount = exist.getAirdropCount() == null ? 0 : exist.getAirdropCount();
            int updatedCount = isAirdropRequest ? existingCount + 1 : existingCount;

            report.setAirdropCount(updatedCount);
            // airdrop 状态与次数保持一致
            report.setAirdrop(updatedCount > 0);
            report.setId(exist.getId());
            report.setSource(report.getSource() - 15);
            reportMapper.updateById(report);
        } else {
            // 不存在 → 插入
            int initialCount = isAirdropRequest ? 1 : 0;
            report.setAirdropCount(initialCount);
            report.setAirdrop(initialCount > 0);
            reportMapper.insert(report);
        }
    }

    @Override
    public List<UserScoreReport> getAllReports() {
        return reportMapper.selectList(null);
    }

    @Override
    public List<UserAirdropInfo> getTodayAirdropUsers() {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("MM-dd"));

        QueryWrapper<UserScoreReport> wrapper = new QueryWrapper<>();
        wrapper.eq("report_date", today);
        wrapper.gt("airdrop_count", 0);

        List<UserScoreReport> reports = reportMapper.selectList(wrapper);
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
