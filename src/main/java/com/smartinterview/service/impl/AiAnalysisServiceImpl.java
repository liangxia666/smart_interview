package com.smartinterview.service.impl;

import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.smartinterview.common.exception.AiServiceException;
import com.smartinterview.service.AiAnalysisService;
import io.reactivex.Flowable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

/**
 * folwable对象 异步非阻塞式接收大模型逐段生成的简历分析数据，一有数据就返回给前端
 */
@Slf4j
@Service
public class AiAnalysisServiceImpl implements AiAnalysisService {
    @Value("${aliyun.dashscope.api-key}")
    private String apiKey;

    /**
     * 分析简历
     * @param rawText
     * @return
     */
    public Flowable<GenerationResult> streamAnalyzeResume(String rawText){
        try {
            //gen对象用于跟通义千问通信
            Generation gen=new Generation();
            //定义 System Prompt（系统指令）：给 AI 设定“身份和工作规则
            String systemPrompt = "你是一个拥有15年经验的BAT资深技术面试官。\n" +
                    "请根据提供的候选人简历文本，进行客观、深度的结构化诊断分析。必须严格按照以下两部分及指定小标题输出：\n\n" +
                    "===第一部分：展示给候选人的评估报告===\n" +
                    "【一、核心优势】\n提炼候选人的技术亮点与岗位匹配度。\n" +
                    "【二、潜在不足】\n指出简历中的薄弱环节、表述不清或容易在真实面试中被质疑的地方。\n" +
                    "【三、面试方向预测】\n基于简历，预测面试官最可能深挖的3个核心技术点或项目难点。仅指出复习方向，严禁直接生成面试问题。\n\n" +
                    "===第二部分：后端系统提取的内部上下文（严格按格式输出）===\n" +
                    "【四、量化评分】\n" +
                    "仅输出一个合法的JSON对象，包含五个维度的评分(0-100)。严禁包含多余的文字、Markdown代码块标记(如```json)、或时间戳等无关数据。\n" +
                    "格式模板：{\"total\":85, \"technical\":80, \"project\":90, \"clarity\":85, \"potential\":85}\n" +
                    "【五、项目摘要】\n" +
                    "重要指令：本段落将作为后续AI面试官的专属System Prompt，必须保留最核心的技术细节和项目场景，字数控制在600-800字左右。必须使用纯客观陈述句，严禁使用对话语气（如'你好'、'请问'）。\n" +
                    "请结构化罗列以下硬核信息：\n" +
                    "1. [技术栈深层锚点]：必须提取具体的技术落地细节（示例：严禁写'熟悉Redis'，应写为'使用Redis Lua脚本结合分布式锁解决高并发超卖'）。\n" +
                    "2. [项目高光与复杂逻辑]：提炼最复杂的业务链路、架构设计或性能优化数据（如QPS提升指标、分表策略）。\n" +
                    "3. [硬核追问靶点]：指出简历中描述单薄或极度考验底层原理的技术点，供后续生成深度连环追问。";
            //构建系统消息
            Message systemMsg=Message.builder()
                    .role(Role.SYSTEM.getValue()) //消息角色就是告诉AI是谁发送的消息
                    .content(systemPrompt) //消息内容
                    .build();
            //构建用户消息
            Message userMsg=Message.builder()
                    .role(Role.USER.getValue())
                    .content("这是候选人的简历纯文本，请进行诊断："+rawText)
                    .build();
            //封装调用AI需要的参数
            GenerationParam param=GenerationParam.builder()
                    .apiKey(apiKey)
                    .model("qwen-turbo") //调用的大模型类型
                    .messages(Arrays.asList(systemMsg,userMsg))
                    //指定AI的返回结果为结构化消息
                    .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                    //开启增量输出,每次只返回最新生成的数据
                    .incrementalOutput(true)
                    .build();
            log.info("开始通义千问大模型SSE流式生成中...");
            return gen.streamCall(param);
        }  catch (Exception e) {
            log.error("调用通义千问大模型分析简历失败",e);
            //自定义异常类
            throw new AiServiceException("AI 智能分析引擎开小差了，请稍后重试");
        }
    }

    /**
     * 从redis拼接历史会话记录
     * @param messages
     * @return
     */
    public Flowable<GenerationResult> streamChat(List<Message> messages){
        try {
            Generation gen=new Generation();
            GenerationParam param=GenerationParam.builder()
                    .apiKey(apiKey)
                    .messages(messages)
                    .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                    .incrementalOutput(true)
                    .model("qwen-turbo")
                    .build();
            log.info("开启多轮对话SSE流生成中....");
            return gen.streamCall(param);
        } catch (Exception e) {
            log.info("AI多轮对话生成失败：{}",e);
            throw new AiServiceException("面试官开小差了，请再说一遍");
        }
    }
}
