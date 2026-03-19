package com.smartinterview.controller;

import com.smartinterview.common.result.Result;
import com.smartinterview.entity.InterviewSession;
import com.smartinterview.service.InterviewReportService;
import com.smartinterview.service.impl.InterviewSessionServiceImpl;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("interview")
@Tag(name="模拟面试中心")
public class InterviewReportController {
    @Autowired
    private InterviewSessionServiceImpl interviewSessionService;
    @Autowired
    private InterviewReportService interviewReportService;

    @PostMapping("report/{sessionId}")
    public Result getReport(@PathVariable Long sessionId){
        log.info("生成面试报告：",sessionId);

        return interviewReportService.buildReport(sessionId);
    }


}
