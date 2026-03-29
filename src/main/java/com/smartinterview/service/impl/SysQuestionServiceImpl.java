package com.smartinterview.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartinterview.entity.QuestionVector;
import com.smartinterview.entity.SysQuestion;
import com.smartinterview.mapper.SysQuestionMapper;
import com.smartinterview.service.SysQuestionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 题库服务：使用向量相似度替代全文索引，解决语义不匹配问题
 *
 * 架构：
 * - 题目向量存储在 MySQL embedding 字段（JSON格式）
 * - 项目启动时加载所有题目向量到内存缓存
 * - 查询时在内存中计算余弦相似度，O(n) 复杂度
 * - 200道题 × 1024维 ≈ 800KB 内存，可以接受
 */
@Service
@Slf4j
public class SysQuestionServiceImpl extends ServiceImpl<SysQuestionMapper, SysQuestion>
        implements SysQuestionService {

    @Autowired
    private EmbeddingServiceImpl embeddingService;

    /**
     * 内存向量缓存：存储所有有 embedding 的题目
     * 使用 CopyOnWriteArrayList 保证线程安全
     */
    private final CopyOnWriteArrayList<QuestionVector> vectorCache = new CopyOnWriteArrayList<>();

    /**
     * 相似度阈值：低于此值认为没有匹配
     * 0.75 表示语义高度相关才返回，避免误匹配
     */
    private static final double SIMILARITY_THRESHOLD = 0.75;

    // ─────────────────────────────────────────────────────────
    //  内存缓存：存 id + question + answer + embedding 向量
    // ─────────────────────────────────────────────────────────



    /**
     * 项目启动时自动加载题目向量到内存
     * 只加载已生成 embedding 的题目
     */
    @PostConstruct
    public void initVectorCache() {
        try {
            List<SysQuestion> questions = lambdaQuery()
                    .isNotNull(SysQuestion::getEmbedding)
                    .ne(SysQuestion::getEmbedding, "") //!=
                    .list();

            for (SysQuestion q : questions) {
                //将数据库中的向量json反序列化为float
                float[] vector = embeddingService.fromJson(q.getEmbedding());
                if (vector != null) {
                    //存储到内存中
                    vectorCache.add(new QuestionVector(
                            q.getId(), q.getQuestion(), q.getAnswer(), vector));
                }
            }
            log.info("向量缓存初始化完成，共加载 {} 道题目", vectorCache.size());
        } catch (Exception e) {
            log.error("向量缓存初始化失败", e);
        }
    }

    // ─────────────────────────────────────────────────────────
    //  核心方法：向量检索标准答案
    // ─────────────────────────────────────────────────────────

    /**
     * 用向量相似度检索最匹配的标准答案
     * 替代原来的全文索引，解决语义不匹配问题
     *
     * @param question  AI的问题（只传 AI 问题，不拼用户回答）
     * @return 匹配的原题+标准答案，未匹配返回 null
     */
    @Override
    public String searchStanderAnswer(String question) {
        if (StrUtil.isBlank(question)) {
            return null;
        }
        // 缓存为空时降级返回 null（不影响主流程）
        if (vectorCache.isEmpty()) {
            log.warn("向量缓存为空，RAG 跳过");
            return null;
        }

        try {
            // 1. 将查询文本转为向量
            float[] queryVector = embeddingService.embed(question);
            if (queryVector == null) {
                log.warn("查询向量生成失败，RAG 跳过");
                return null;
            }

            // 2. 遍历缓存，计算余弦相似度，找最相似的题目，不查找数据库
            double maxSimilarity = -1;
            QuestionVector bestMatch = null;

            for (QuestionVector qv : vectorCache) {
                //计算余弦相似度
                double similarity = embeddingService.cosineSimilarity(queryVector, qv.getVector());
                if (similarity > maxSimilarity) {
                    maxSimilarity = similarity;
                    bestMatch = qv;
                }
            }

            // 3. 相似度超过阈值才返回
            if (bestMatch != null && maxSimilarity >= SIMILARITY_THRESHOLD) {
                log.info("RAG命中：相似度={}，题目={}", String.format("%.3f",maxSimilarity),
                        bestMatch.getQuestion().substring(0, Math.min(30, bestMatch.getQuestion().length())));
                return  bestMatch.getAnswer();
            }

            log.info("RAG未命中：最高相似度={}，低于阈值{}", String.format("%.3f",maxSimilarity), SIMILARITY_THRESHOLD);
            return null;

        } catch (Exception e) {
            log.error("向量检索异常，降级跳过", e);
            return null;
        }
    }

    // ─────────────────────────────────────────────────────────
    //  管理方法：为题目批量生成 embedding
    // ─────────────────────────────────────────────────────────

    /**
     * 为所有没有 embedding 的题目批量生成向量
     * 调用一次即可，后续新增题目通过 generateEmbeddingForQuestion 单独生成
     * 建议在题库导入完成后手动调用此接口
     */
    @Async
    public void batchGenerateEmbedding() {
        List<SysQuestion> questions = lambdaQuery()
                .isNull(SysQuestion::getEmbedding)
                .or()
                .eq(SysQuestion::getEmbedding, "")
                .list();

        log.info("开始批量生成向量，共 {} 道题目", questions.size());
        int success = 0, fail = 0;

        for (SysQuestion q : questions) {
            try {
              //计算问题的向量
                String text = q.getQuestion();
                float[] vector = embeddingService.embed(text);

                if (vector != null) {
                    // 存库
                    SysQuestion update = new SysQuestion();
                    update.setId(q.getId());
                    update.setEmbedding(embeddingService.toJson(vector));
                    updateById(update);

                    // 更新内存缓存
                    vectorCache.add(new QuestionVector(
                            q.getId(), q.getQuestion(), q.getAnswer(), vector));
                    success++;
                } else {
                    fail++;
                }

                // 每次调用间隔 100ms，避免触发 API 限流
                Thread.sleep(100);

            } catch (Exception e) {
                log.error("题目 {} 生成向量失败", q.getId(), e);
                fail++;
            }
        }
        log.info("批量生成向量完成：成功 {}，失败 {}", success, fail);
    }

    /**
     * 为单道题目生成 embedding（题库导入后调用）
     * 在 QuestionDataListener.doAfterAllAnalysed() 后触发
     */
    public void generateEmbeddingForQuestion(SysQuestion q) {
        try {
            String text = q.getQuestion();
            float[] vector = embeddingService.embed(text);
            if (vector != null) {
                SysQuestion update = new SysQuestion();
                update.setId(q.getId());
                update.setEmbedding(embeddingService.toJson(vector));
                updateById(update);
                // 同步更新内存缓存
                vectorCache.add(new QuestionVector(
                        q.getId(), q.getQuestion(), q.getAnswer(), vector));
            }
        } catch (Exception e) {
            log.error("单题生成向量失败，questionId={}", q.getId(), e);
        }
    }
}



