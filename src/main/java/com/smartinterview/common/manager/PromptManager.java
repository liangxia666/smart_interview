package com.smartinterview.common.manager;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;

/**
 * 集中管理系统所有的 AI 提示词模板
 */
@Slf4j
@Component
public class PromptManager {

    // 注入 resources/prompts 下的文件
    @Value("classpath:prompts/resume-analysis-system.st")
    private Resource resumeAnalysisResource;

    @Value("classpath:prompts/resume-score-system.st")
    private Resource resumeScoreResource;
    @Value("classpath:prompts/interview-chat-system.st")
    private Resource interviewChatResource;
    @Value("classpath:prompts/evaluate-answer.st")
    private Resource evaluateAnswerResource;


    // 缓存在内存中的字符串，避免每次请求都去读文件
    private String resumeAnalysisTemplate;
    private String resumeScoreTemplate;
    private String interviewChatTemplate;
    private String evaluateAnswerTemplate;
    /**
     * 项目启动时自动将文件内容加载到内存中
     */
    @PostConstruct
    public void init() {
        try {
            //将文件输入流读取成字符串，指定UTF-8编码
            resumeAnalysisTemplate = IoUtil.read(resumeAnalysisResource.getInputStream(), StandardCharsets.UTF_8);
            resumeScoreTemplate    = IoUtil.read(resumeScoreResource.getInputStream(), StandardCharsets.UTF_8);
            interviewChatTemplate = IoUtil.read(interviewChatResource.getInputStream(), StandardCharsets.UTF_8);
            evaluateAnswerTemplate=IoUtil.read(evaluateAnswerResource.getInputStream(),StandardCharsets.UTF_8);
            log.info("AI Prompt 模板加载完成！");
        } catch (Exception e) {
            log.error("AI Prompt 模板加载失败，请检查文件路径！", e);
        }
    }

    /**
     * 获取静态的简历分析提示词
     */
    public String getResumeAnalysisSystemPrompt() {
        return resumeAnalysisTemplate;
    }
    public String getResumeScoreSystemPrompt() {
        return resumeScoreTemplate;
    }


    /**
     * 动态构建面试官提示词（替换占位符）
     */
    public String buildInterviewChatSystemPrompt(String summaryText,
                                                 String ragContext,
                                                 String difficulty,
                                                 String jobIntention) {
        String resumeSummary = summaryText != null ? summaryText : "暂无简历画像";

        //查到标准答案就组装，没查到就留空
        String ragPrompt= StrUtil.isNotBlank(ragContext)
                ? "\n【标准参考答案】：\n" + ragContext
                : "";
        String difficultyPrompt = StrUtil.isNotBlank(difficulty)
                ? "\n【本次面试难度】：" + difficulty +
                "，提问深度、术语复杂度和追问力度必须严格匹配该级别。"
                : "";
       String intentionPrompt = StrUtil.isNotBlank(jobIntention)
                ? "\n【候选人求职意向】：" + jobIntention +
                "，你的提问方向和深度必须严格匹配该岗位的技术要求，" +
                "不相关的技术领域不要涉及。"
                : "";


        return interviewChatTemplate
                .replace("{{summaryText}}", resumeSummary)
                .replace("{{standardAnswer}}",ragPrompt)
                .replace("{{difficultyContext}}",difficultyPrompt)
                .replace("{{jobIntention}}",intentionPrompt);
    }
    public String buildEvaluationPrompt(String aiQuestion,String userAnswer,String standardAnswer){
        String userAnswerFinal = StrUtil.isNotBlank(userAnswer) ? userAnswer : "（候选人未作答）";
        String stdAnswerFinal=StrUtil.isNotBlank(standardAnswer)?standardAnswer:"（无标准答案，请依据业界最佳实践评判）";
        return evaluateAnswerTemplate.replace("{{aiQuestion}}",aiQuestion)
                .replace("{{userAnswer}}",userAnswerFinal)
                .replace("{{stdPart}}",stdAnswerFinal);
    }

}