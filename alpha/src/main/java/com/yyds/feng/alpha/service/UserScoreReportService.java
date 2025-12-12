package com.yyds.feng.alpha.service;

import com.yyds.feng.alpha.entity.UserScoreReport;
import com.yyds.feng.alpha.service.dto.UserAirdropInfo;
import java.util.List;

public interface UserScoreReportService {
    void saveReport(UserScoreReport report);
    
    List<UserScoreReport> getAllReports();

    /**
     * 获取当日已提交空投的用户名及次数
     */
    List<UserAirdropInfo> getTodayAirdropUsers();
}
