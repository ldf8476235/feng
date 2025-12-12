package com.yyds.feng.lighter.controller;

import com.yyds.feng.common.util.R;
import com.yyds.feng.lighter.entity.LighterLog;
import com.yyds.feng.lighter.model.SaveLogRequest;
import com.yyds.feng.lighter.service.LighterLogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
public class LighterLogController {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MM-dd");

    @Resource
    private LighterLogService lighterLogService;

    @PostMapping("/saveLog")
    public R saveLog(@RequestBody SaveLogRequest request) {
        if (request == null || request.getContent() == null || request.getContent().trim().isEmpty()) {
            return R.error("content不能为空");
        }
        lighterLogService.saveLog(request.getContent().trim());
        return R.ok();
    }

    @GetMapping("/getLog")
    public R getLog(@RequestParam(value = "date", required = false) String date) {
        List<LighterLog> logs = lighterLogService.listByDate(date);
        return R.ok(logs);
    }
}
