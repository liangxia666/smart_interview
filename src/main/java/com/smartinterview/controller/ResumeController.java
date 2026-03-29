package com.smartinterview.controller;

import cn.hutool.core.lang.UUID;
import com.smartinterview.common.result.PageResult;
import com.smartinterview.common.result.Result;
import com.smartinterview.common.util.AliOssUtil;
import com.smartinterview.common.util.UserHolder;
import com.smartinterview.config.RabbitConfig;
import com.smartinterview.entity.ResumeAnalysis;
import com.smartinterview.service.ResumeAnalysisService;
import com.smartinterview.vo.ResumeDetailVO;
import com.smartinterview.vo.ResumeUploadVO;
import com.smartinterview.vo.ResumeVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("resume")
@Slf4j
@Tag(name="简历模块")
public class ResumeController {
    @Autowired
    private ResumeAnalysisService resumeAnalysisService;


    @Operation(summary = "上传简历")
    @PostMapping("upload")
    public Result uploadResume(@RequestParam("file") MultipartFile file) {
        log.info("开始上传简历：{}",file.getOriginalFilename());
        ResumeUploadVO upload = resumeAnalysisService.upload(file);
        return Result.success(upload);
    }

    /**
     * 分析简历，处理SSe请求，返回流式文本数据给前端,一旦有数据生成就返回
     * @param resumeId
     * @return
     */
    @Operation(summary="简历AI流式分析（前台轨）")
    //produces返回给前端为流式文本数据
    @GetMapping(value="/ai/stream/{resumeId}",produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamAiAnalysis(@PathVariable("resumeId") Long resumeId, HttpServletResponse response){
        //服务器转字节时采用UTF-8
        response.setCharacterEncoding("UTF-8");
        //告知前端传送流式文本数据，采用UTF--8编码
       response.setContentType("text/event-stream;charset=UTF-8");
       log.info("开始分析简历，简历id:{}",resumeId);
        return resumeAnalysisService.streamAiAnalysis(resumeId);

    }
    @Operation(summary="查询简历评分和摘要")
    @GetMapping("detail/{resumeId}")
    public  Result queryDetail(@PathVariable Long resumeId){
        ResumeDetailVO resumeDetail = resumeAnalysisService.getResumeDetail(resumeId);
        return Result.success(resumeDetail);

    }
    @Operation(summary="查询简历状态")
    @GetMapping("status/{resumeId}")
    public Result<Integer> getResumeStatus(@PathVariable("resumeId")Long resumeId){
        ResumeAnalysis resume = resumeAnalysisService.getById(resumeId);
        if(resume==null){
            return Result.error("找不到改简历");
        }
        return Result.success(resume.getStatus());
    }
//    @Operation(summary="查询简历列表")
//    @GetMapping("page")
//    public Result pageQuery(@RequestParam(defaultValue="1") Integer current,@RequestParam(defaultValue = "10")Integer size){
//      PageResult pageResult=  resumeAnalysisService.pageQuery(current,size);
//      return Result.success(pageResult);
//    }
    @Operation(summary="查询简历列表")
    @GetMapping("list")
    public Result queryResume(){
       List<ResumeVO> resumeVO =resumeAnalysisService.queryResume();
       return Result.success(resumeVO);

    }
    @Operation(summary="逻辑删除")
    @DeleteMapping("{resumeId}")
    public Result LogicalDelete(@PathVariable Long resumeId){
        resumeAnalysisService.logicalDelete(resumeId);
        return Result.success();
    }



}