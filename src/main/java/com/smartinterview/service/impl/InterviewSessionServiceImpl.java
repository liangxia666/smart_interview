package com.smartinterview.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartinterview.common.constants.RedisConstants;
import com.smartinterview.entity.ChatMessage;
import com.smartinterview.entity.InterviewSession;
import com.smartinterview.entity.ResumeAnalysis;
import com.smartinterview.service.AiAnalysisService;
import com.smartinterview.service.InterviewSessionService;
import com.smartinterview.mapper.InterviewSessionMapper;
import io.reactivex.Flowable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
* @author 32341
* @description 针对表【interview_session(面试会话主表)】的数据库操作Service实现
* @createDate 2026-02-26 16:36:05
*/
@Service
public class InterviewSessionServiceImpl extends ServiceImpl<InterviewSessionMapper, InterviewSession>
    implements InterviewSessionService{
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private ChatMessageServiceImpl chatMessageService;
    @Autowired
    private AiAnalysisServiceImpl aianalysisService;
    @Autowired
    private ResumeAnalysisServiceImpl resumeAnalysisService;
    private static final int MAX_HISTORY_MSG=10;

    /**
     * 由系统提示词+历史会话+当前用户会话共同组成消息集合传到AI生成流式文本
     * @param sessionId
     * @param userMessage
     * @return
     */
    @Override
    public SseEmitter chat(Long sessionId, String userMessage) {

        SseEmitter emitter = new SseEmitter(60000L);
       String redisKey = RedisConstants.INTERVIEW_CHAT_HISTORY+sessionId;
       List<Message> messages=new ArrayList();
       //摘要
       String summaryText=getSummary(sessionId);
       //系统提示词
        // 系统提示词
        String systemPrompt = "你是一个拥有15年经验的大厂技术面试官。以下是该候选人的简历核心画像：\n"
                + summaryText +
                "\n【面试规则】：\n"
                + "1. 严禁重复提问已经讨论过的问题或知识点。\n"
                + "2. 每一轮提问必须基于候选人的上一次回答进行针对性深挖，或者切换到另一个尚未涉及的技术维度。\n"
                + "3. 如果发现候选人已经回答得非常透彻，请立刻转换话题。\n"
                + "4. 每次只提问1个具体的技术问题，切勿长篇大论。请以专业、严厉的语气进行考核。"
                + "5.严禁出现'你好'、'很高兴为你面试'等寒暄,直接进入技术对线";
       messages.add(Message.builder().role(Role.SYSTEM.getValue()).content(systemPrompt).build());
       //查询当前会话框的历史对话
        List<String> range = stringRedisTemplate.opsForList().range(redisKey, 0, -1);
        //在添加到消息中
        if(range!=null&&!range.isEmpty()){
            for(String s:range){
                Message msg= JSONUtil.toBean(s,Message.class,false);
                messages.add(msg);
            }
        }
        //添加本次用户消息
        Message currentUserMsg=Message.builder().role(Role.USER.getValue()).content(userMessage).build();
        messages.add(currentUserMsg);
        //将用户的发言先异步存入mysql
        saveMessage(sessionId,Role.USER.getValue(),userMessage);
        //调用AI开启流式聊天
        Flowable<GenerationResult> flowable=aianalysisService.streamChat(messages);
        //拼接字符串
        StringBuilder aiResponseBuffer=new StringBuilder();
        flowable.subscribe(
                result -> {
                    String chunk=result.getOutput().getChoices().get(0).getMessage().getContent();
                    if(StrUtil.isNotBlank(chunk)){
                        aiResponseBuffer.append(chunk);
                        try {
                            emitter.send(chunk);
                        } catch (IOException e) {
                            emitter.completeWithError(e);
                        }
                    }
                },
                error -> {
                    log.error("模拟面试发生异常:{}",error);
                    try {
                        emitter.completeWithError(error);
                    } catch (Exception e) {

                    }
                },
                ()->{
                    String aiFullResponse=aiResponseBuffer.toString();
                    //创建AI消息
                    Message aiMessage=Message.builder().role(Role.ASSISTANT.getValue()).content(aiFullResponse).build();
                    //一问一答添加到redis
                    stringRedisTemplate.opsForList().rightPush(redisKey, JSONUtil.toJsonStr(currentUserMsg));
                    stringRedisTemplate.opsForList().rightPush(redisKey,JSONUtil.toJsonStr(aiMessage));
                    //滑动窗口裁剪，只保留最近N条数据，防止大模型token爆炸
                    stringRedisTemplate.opsForList().trim(redisKey,-MAX_HISTORY_MSG,-1);
                    //刷新过期时间
                    stringRedisTemplate.expire(redisKey,RedisConstants.INTERVIEW_CHAT_TTL, TimeUnit.MINUTES);
                    //将AI消息保存到数据库
                    saveMessage(sessionId,Role.ASSISTANT.getValue(), aiFullResponse);
                    emitter.send("DONE");
                    emitter.complete();
                }

        );
        return emitter;
    }
    public void saveMessage(Long sessionId,String role,String content){
        ChatMessage chatMessage=ChatMessage.builder()
                .sessionId(sessionId)
                .content(content)
                .role(role)
                .createTime(LocalDateTime.now())
                .build();
        chatMessageService.save(chatMessage);

    }
    private String getSummary(Long sessionId){
        InterviewSession session = getById(sessionId);
        if(session==null){
            return "简历找不到，请直接开始技术相关的面试";
        }
        Long resumeId = session.getResumeId();
        ResumeAnalysis resumeAnalysis =resumeAnalysisService.getById(resumeId);
        String summary=resumeAnalysis.getSummary();
        return summary;
    }
}




