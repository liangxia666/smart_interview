package com.smartinterview.Listener;

import com.smartinterview.common.util.ResumeParser;
import com.smartinterview.config.RabbitConfig;
import com.smartinterview.entity.ResumeAnalysis;
import com.smartinterview.service.ResumeAnalysisService;
import com.smartinterview.service.impl.AiAnalysisServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
public class ResumeParseListener {
    @Autowired
    private ResumeAnalysisService resumeAnalysisService;
    @Autowired
    private ResumeParser resumeParser;

    @RabbitListener(queues = RabbitConfig.RESUME_PARSE_QUEUE)
    public void handlerResumeParse(Long resumeId) {
        log.info("消费者开始处理简历解析任务，简历ID: {}", resumeId);
        // 1. 从数据库查出对应的记录和 OSS 地址
        ResumeAnalysis resume = resumeAnalysisService.getById(resumeId);
        String fileUrl = resume.getFileUrl();
        try {
            // 2. 调用 PDFBox 工具类提取文本
            String text = resumeParser.parsePdfFromUrl(fileUrl);
            // 3. 模拟调用大模型或深度分析的超长耗时操作 (休眠 3 秒)
            //String s = aiAnalysisService.streamAnalyzeResume(text);
            //System.out.println("AI分析的内容："+s);
            // 4. 更新数据库状态为 1 (解析成功)，并保存纯文本
            resume.setOriginalText(text);
            resume.setStatus(1);
            resume.setUpdateTime(LocalDateTime.now());
            resumeAnalysisService.updateById(resume);
        } catch (Exception e) {
            // 解析失败，更新状态为 -1，方便前端展示“解析失败，请重试”
            log.error("简历解析发生致命异常，简历ID: {}", resumeId, e);
            resume.setStatus(-1);
            resumeAnalysisService.updateById(resume);
        } finally {
        }

    }
}