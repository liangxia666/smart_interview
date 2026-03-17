package com.smartinterview.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartinterview.common.constants.RedisConstants;
import com.smartinterview.common.manager.PromptManager;
import com.smartinterview.entity.ChatMessage;
import com.smartinterview.entity.InterviewSession;
import com.smartinterview.entity.ResumeAnalysis;
import com.smartinterview.service.AiAnalysisService;
import com.smartinterview.service.InterviewSessionService;
import com.smartinterview.mapper.InterviewSessionMapper;
import com.smartinterview.service.SysQuestionService;
import io.reactivex.Flowable;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
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
    @Autowired
    private PromptManager promptManager;
    private static final int MAX_HISTORY_MSG=10;
    @Autowired
    private SysQuestionService sysQuestionService;
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

       //========获取历史消息=========
        List<Message> historyMessage=new ArrayList<>();
        //查询当前会话框的历史对话
        List<String> range = stringRedisTemplate.opsForList().range(redisKey, 0, -1);
        //在添加到消息中
        if(range!=null&&!range.isEmpty()){
            for(String s:range){
                Message msg= JSONUtil.toBean(s,Message.class,false);
                historyMessage.add(msg);
            }
        }
       //提取AI上一条问题
        String lastAiQuestion="";
        //集合只声明的话为null
        if(historyMessage!=null&&!historyMessage.isEmpty()){
            Message lastMsg= historyMessage.get(messages.size()-1);
            if(Role.ASSISTANT.getValue().equals(lastMsg.getRole())){
                lastAiQuestion=lastMsg.getContent();
            }
        }

        //将AI的问题跟用户的回答拼接在一起作为检索的依据
        String searchQuery=lastAiQuestion+" "+userMessage;
       //获取标准答案
        String standerAnswer=sysQuestionService.searchStanderAnswer(searchQuery);
        if(standerAnswer!=null){
            log.info("RAG命中标准答案：{}",standerAnswer);
        }else{
            log.info("RAG未命中，大模型将自由发挥");
        }
        //摘要
        String summaryText=getSummary(sessionId);
        // 系统提示词
        String systemPrompt = promptManager.buildInterviewChatSystemPrompt(summaryText,standerAnswer);
        //添加系统提示此消息
        messages.add(Message.builder().role(Role.SYSTEM.getValue()).content(systemPrompt).build());
        //添加历史消息
        messages.addAll(historyMessage);
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




