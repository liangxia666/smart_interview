package com.smartinterview.controller;

import com.smartinterview.common.result.Result;
import com.smartinterview.entity.InterviewSession;
import com.smartinterview.service.InterviewReportService;
import com.smartinterview.service.impl.InterviewSessionServiceImpl;
import com.smartinterview.vo.InterviewReportVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("interview")
@Tag(name="模拟面试中心")
public class InterviewReportController {

    @Autowired
    private InterviewReportService interviewReportService;

    @Operation(summary="生成面试报告")
    @PostMapping("report/{sessionId}")
    public Result getReport(@PathVariable Long sessionId){
        log.info("生成面试报告：{}",sessionId);
        InterviewReportVO interviewReportVO = interviewReportService.buildReport(sessionId);
        return Result.success(interviewReportVO);
    }
    @Operation(summary="导出报告")
    @GetMapping("/{sessionId}/export")
    public void exportReport(@PathVariable(value="sessionId")Long sessionId, HttpServletResponse response){
        log.info("到处面试报告：{}",sessionId);
        interviewReportService.exportReport(sessionId,response);

    }


}
