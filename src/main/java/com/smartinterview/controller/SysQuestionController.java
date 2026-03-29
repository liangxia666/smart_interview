package com.smartinterview.controller;

import com.smartinterview.common.result.Result;
import com.smartinterview.service.impl.SysQuestionServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.alibaba.excel.EasyExcel;
import com.smartinterview.Listener.QuestionDataListener;
import com.smartinterview.service.SysQuestionService;

/**
 * 题库管理 Controller
 */
@RestController
@RequestMapping("/question")
@Slf4j
@Tag(name = "题库管理")
public class SysQuestionController {

    @Autowired
    private SysQuestionService sysQuestionService;

    @Autowired
    private SysQuestionServiceImpl sysQuestionServiceImpl;


    /**
     * 导入题库 Excel
     * 导入完成后会异步触发批量向量生成
     *
     * POST /question/import
     * 实现思路：导入题目时 → 调通义千问 Embedding API → 得到向量 → 存 MySQL(JSON)
     * 查询时     → 查询文本转向量 → 从 MySQL 取所有向量 → Java 计算余弦相似度 → 返回最相似题目
     */
    @Operation(summary = "导入题库 Excel")
    @PostMapping("/import")
    public Result importQuestion(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.error("文件不能为空");
        }
        try {
            EasyExcel.read(
                    file.getInputStream(),
                    com.smartinterview.dto.QuestionImportDTO.class,
                    new QuestionDataListener(sysQuestionService)
            ).sheet().doRead();//读取第一个sheet，开始执行

            // 导入完成后异步为新题目生成向量
            sysQuestionServiceImpl.batchGenerateEmbedding();

            return Result.success("题库导入成功，正在后台生成向量索引...");
        } catch (Exception e) {
            log.error("题库导入失败", e);
            return Result.error("题库导入失败：" + e.getMessage());
        }
    }

    /**
     * 手动触发批量生成向量（已有题目但还没有 embedding 时使用）
     * 项目第一次上线时调用一次即可
     *
     * POST /question/embedding/batch
     */
    @Operation(summary = "批量生成题目向量（首次初始化）")
    @PostMapping("/embedding/batch")
    public Result batchGenerateEmbedding() {
        sysQuestionServiceImpl.batchGenerateEmbedding();
        return Result.success("已在后台启动向量生成任务，请查看日志进度");
    }
}