package com.yyds.feng.alpha.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yyds.feng.alpha.entity.UserScoreReport;
import com.yyds.feng.alpha.mapper.UserScoreReportMapper;
import com.yyds.feng.alpha.service.UserScoreReportService;
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
            // 已存在 → 执行更新
            report.setId(exist.getId());
            reportMapper.updateById(report);
        } else {
            // 不存在 → 插入
            reportMapper.insert(report);
        }
    }
    
    @Override
    public List<UserScoreReport> getAllReports() {
        return reportMapper.selectList(null);
    }

    @Override
    public List<String> getTodayReportUsers() {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("MM-dd"));

        QueryWrapper<UserScoreReport> wrapper = new QueryWrapper<>();
        wrapper.eq("report_date", today);

        List<UserScoreReport> reports = reportMapper.selectList(wrapper);
        return reports.stream()
                .map(UserScoreReport::getUsername)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(name -> !name.isEmpty())
                .distinct()
                .collect(Collectors.toList());
    }
}
