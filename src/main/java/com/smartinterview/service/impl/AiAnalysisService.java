package com.smartinterview.service.impl;

import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.smartinterview.common.exception.AiServiceException;
import io.reactivex.Flowable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;

/**
 * folwable对象 异步非阻塞式接收大模型逐段生成的简历分析数据，一有数据就返回给前端
 */
@Slf4j
@Service
public class AiAnalysisService {
    @Value("${aliyun.dashscope.api-key}")
    private String apiKey;
    public Flowable<GenerationResult> streamAnalyzeResume(String rawText){
        try {
            //gen对象用于跟通义千问通信
            Generation gen=new Generation();
            //定义 System Prompt（系统指令）：给 AI 设定“身份和工作规则”
            String systemPrompt = "你是一个拥有15年经验的 BAT 资深技术大厂 HR 兼技术面试官。" +
                    "请根据我提供的候选人简历文本，进行专业、客观、深度的结构化分析。" +
                    "请按以下格式输出（必须包含）：\n" + // \n 是换行符，让 AI 输出时换行
                    "【一、核心优势】(提取候选人的技术亮点和闪光点)\n" +
                    "【二、潜在不足】(指出简历中的薄弱环节或项目经验的欠缺)\n" +
                    "【三、定制化面试题】(根据其技能栈，给出3-5个由浅入深的技术面试提问建议)";
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
//            //发送消息给大模型，程序等待响应结果
//            GenerationResult result=gen.call(param);
//            //      解析 AI 返回的结果：一步步拿到最终的分析文本
//            //     result.getOutput()：获取 AI 返回的核心输出内容
//            //     getChoices()：获取 AI 的回答列表（默认只有1个回答）
//            //     get(0)：取第一个回答（因为我们只需要一个分析结果）
//            //     getMessage()：获取回答的 Message 对象
//            //     getContent()：获取 Message 中的文本内容（就是 AI 生成的分析报告）
//            String aiResult=result.getOutput().getChoices().get(0).getMessage().getContent();
//            log.info("大模型分析完成！字数：{}",aiResult.length());
        }  catch (Exception e) {
            log.error("调用通义千问大模型分析简历失败",e);
            //自定义异常类
            throw new AiServiceException("AI 智能分析引擎开小差了，请稍后重试");
        }
    }
}
