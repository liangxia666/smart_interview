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
@RequestMapping("interview/report")
@Tag(name="面试报告模块")
public class InterviewReportController {

    @Autowired
    private InterviewReportService interviewReportService;

    @Operation(summary="查看面试报告")
    @PostMapping("{sessionId}")
    public Result getReport(@PathVariable(value="sessionId") Long sessionId){
        log.info("查看面试报告：{}",sessionId);
        InterviewReportVO interviewReportVO = interviewReportService.buildReport(sessionId);
        return Result.success(interviewReportVO);
    }
    @Operation(summary="导出PDF报告")
    @GetMapping("export/{sessionId}")
    public void exportReport(@PathVariable(value="sessionId")Long sessionId, HttpServletResponse response){
        log.info("导出面试报告：{}",sessionId);
        interviewReportService.exportReport(sessionId,response);
    }

}
