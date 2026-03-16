package com.smartinterview.service.impl;

import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.smartinterview.common.exception.AiServiceException;
import com.smartinterview.manager.PromptManager;
import com.smartinterview.service.AiAnalysisService;
import io.reactivex.Flowable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
    @Autowired
    private PromptManager promptManager;

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
            String systemPrompt =promptManager.getResumeAnalysisSystemPrompt();
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
