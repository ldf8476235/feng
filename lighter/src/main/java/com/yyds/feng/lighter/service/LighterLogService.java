package com.yyds.feng.lighter.service;

import com.yyds.feng.lighter.entity.LighterLog;

import java.util.List;

public interface LighterLogService {

    /**
     * Save a log with current date/time.
     */
    void saveLog(String content);

    /**
     * List logs by date (MM-dd), defaults to today when blank.
     */
    List<LighterLog> listByDate(String dateMd);
}
