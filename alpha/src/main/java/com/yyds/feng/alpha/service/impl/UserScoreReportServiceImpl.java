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
}