package com.smartinterview.service;

import com.smartinterview.common.result.Result;
import com.smartinterview.entity.ResumeAnalysis;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
* @author 32341
* @description 针对表【resume_analysis(简历智能分析表)】的数据库操作Service
* @createDate 2026-02-26 16:36:05
*/
public interface ResumeAnalysisService extends IService<ResumeAnalysis> {

    Result upload(MultipartFile file, String intention);

    SseEmitter streamAiAnalysis(Long resumeId);
}
