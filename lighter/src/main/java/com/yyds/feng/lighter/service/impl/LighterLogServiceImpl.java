package com.yyds.feng.lighter.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yyds.feng.lighter.entity.LighterLog;
import com.yyds.feng.lighter.mapper.LighterLogMapper;
import com.yyds.feng.lighter.service.LighterLogService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class LighterLogServiceImpl implements LighterLogService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MM-dd");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    @Resource
    private LighterLogMapper lighterLogMapper;

    @Override
    public void saveLog(String content) {
        LocalDateTime now = LocalDateTime.now();

        LighterLog log = new LighterLog();
        log.setContent(content);
        log.setDateMd(now.format(DATE_FMT));
        log.setTimeHm(now.format(TIME_FMT));

        lighterLogMapper.insert(log);
    }

    @Override
    public List<LighterLog> listByDate(String dateMd) {
        String targetDate = (dateMd == null || dateMd.trim().isEmpty())
                ? LocalDate.now().format(DATE_FMT)
                : dateMd.trim();
        QueryWrapper<LighterLog> wrapper = new QueryWrapper<>();
        wrapper.eq("date_md", targetDate);
        wrapper.orderByDesc("time_hm");
        wrapper.orderByDesc("id");

        return lighterLogMapper.selectList(wrapper);
    }
}
