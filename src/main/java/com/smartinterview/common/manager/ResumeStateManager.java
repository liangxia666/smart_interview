package com.smartinterview.common.manager;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.smartinterview.entity.ResumeAnalysis;
import com.smartinterview.mapper.ResumeAnalysisMapper;
import com.smartinterview.service.ResumeAnalysisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

@Component
@Slf4j
public class ResumeStateManager {

    @Autowired
    private ResumeAnalysisMapper resumeAnalysisMapper; // 你的原Service或Mapper

    /**
     * 前台轨完成：保存 AI 文本并更新状态为 2
     */
    public void updateToTextGenerated(Long resumeId, String aiResult) {
        try {
            ResumeAnalysis resume = resumeAnalysisMapper.selectById(resumeId);
            if (resume != null) {
                resume.setAiResult(aiResult);
                resume.setStatus(2);
                resume.setUpdateTime(LocalDateTime.now());
                resumeAnalysisMapper.updateById(resume);
            }
        } catch (Exception e) {
            log.error("ai_result存库失败，resumeId={}", resumeId, e);
        }
    }

    /**
     * 后台轨完成：解析 JSON 并更新状态为 3
     */
    public void updateToScored(Long resumeId, String jsonRaw) {
        try {
            // 脏活累活：安全提取 JSON
            int start = jsonRaw.indexOf("{");
            int end = jsonRaw.lastIndexOf("}");
            if (start == -1 || end == -1) {
                log.error("后台轨JSON格式异常，resumeId={}, raw={}", resumeId, jsonRaw);
                markAsFailed(resumeId);
                return;
            }

            JSONObject json = JSONUtil.parseObj(jsonRaw.substring(start, end + 1));
            String scoreJson = json.getJSONObject("score") != null ? json.getJSONObject("score").toString() : "{}";
            String summary = json.getStr("summary", "暂无摘要");

            ResumeAnalysis resume = resumeAnalysisMapper.selectById(resumeId);
            resume.setScore(scoreJson);
            resume.setSummary(summary);
            resume.setStatus(3);
            resume.setUpdateTime(LocalDateTime.now());
            resumeAnalysisMapper.updateById(resume);
            log.info("后台轨完成，resumeId={}，status已置为3", resumeId);

        } catch (Exception e) {
            log.error("后台轨评分解析或存库失败，resumeId={}", resumeId, e);
            markAsFailed(resumeId);
        }
    }

    /**
     * 标记为失败状态 (-1)
     */
    public void markAsFailed(Long resumeId) {
        ResumeAnalysis resume = resumeAnalysisMapper.selectById(resumeId);
        if (resume != null) {
            resume.setStatus(-1);
            resume.setUpdateTime(LocalDateTime.now());
            resumeAnalysisMapper.updateById(resume);
        }
    }
}