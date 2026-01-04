package com.yyds.feng.alpha.service;

import com.yyds.feng.alpha.entity.UserScoreReportVd;
import com.yyds.feng.alpha.service.dto.UserAirdropInfo;

import java.util.List;

public interface UserScoreReportVdService {
    void saveReport(UserScoreReportVd report);

    List<UserScoreReportVd> getAllReports();

    /**
     * Get today's airdrop users and counts.
     */
    List<UserAirdropInfo> getTodayAirdropUsers();
}
