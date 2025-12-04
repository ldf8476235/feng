package com.yyds.feng.alpha.service;

import com.yyds.feng.alpha.entity.UserScoreReport;
import java.util.List;

public interface UserScoreReportService {
    void saveReport(UserScoreReport report);
    
    List<UserScoreReport> getAllReports();
}