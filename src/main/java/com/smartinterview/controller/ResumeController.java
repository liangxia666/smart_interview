package com.smartinterview.controller;

import cn.hutool.core.lang.UUID;
import com.smartinterview.common.result.Result;
import com.smartinterview.common.util.AliOssUtil;
import com.smartinterview.common.util.UserHolder;
import com.smartinterview.config.RabbitConfig;
import com.smartinterview.entity.ResumeAnalysis;
import com.smartinterview.service.ResumeAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("resume")
@Slf4j
public class ResumeController {
    @Autowired
    private ResumeAnalysisService resumeAnalysisService;

    @Operation(summary = "上传简历")
    @PostMapping("upload")
    public Result uploadResume(@RequestParam("file") MultipartFile file,
                               @RequestParam(value = "intention", required = false,
                                       defaultValue = "Java软件开发") String intention) {
        return resumeAnalysisService.upload(file, intention);
    }

    /**
     * 分析简历，处理SSe请求，返回流式文本数据给前端,一旦有数据生成就返回
     * @param resumeId
     * @return
     */
    @Operation(summary="发送简历AI获取流式回复的分析")
    //produces返回给前端为流式文本数据
    @GetMapping(value="/ai/stream/{resumeId}",produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamAiAnalysis(@PathVariable("resumeId") Long resumeId, HttpServletResponse response){
        //spring转字节时采用UTF-8
        response.setCharacterEncoding("UTF-8");
        //告知前端传送流式文本数据，采用UTF--8编码
       response.setContentType("text/event-stream;charset=UTF-8");
        return resumeAnalysisService.streamAiAnalysis(resumeId);

    }
    @Operation(summary="查询订单状态")
    @GetMapping("status/{resumeId}")
    public Result<Integer> getResumeStatus(@PathVariable("resumeId")Long resumeId){
        ResumeAnalysis resume = resumeAnalysisService.getById(resumeId);
        if(resume==null){
            return Result.error("找不到改简历");
        }
        return Result.success(resume.getStatus());
    }


}