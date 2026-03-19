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
import com.smartinterview.common.result.Result;
import com.smartinterview.common.util.UserHolder;
import com.smartinterview.dto.StartInterviewDTO;
import com.smartinterview.entity.ChatMessage;
import com.smartinterview.entity.InterviewSession;
import com.smartinterview.entity.ResumeAnalysis;
import com.smartinterview.service.AiAnalysisService;
import com.smartinterview.service.InterviewReportService;
import com.smartinterview.service.InterviewSessionService;
import com.smartinterview.mapper.InterviewSessionMapper;
import com.smartinterview.service.SysQuestionService;
import io.reactivex.Flowable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    @Autowired
    private InterviewReportService interviewReportService;


    public Result  startInterview(StartInterviewDTO dto){
        ResumeAnalysis resume=resumeAnalysisService.getById(dto.getResumeId());
        if(resume==null){
            return Result.error("找不到该简历");
        }
        if(resume.getStatus()<2||resume.getStatus()==null){
            return Result.error("简历尚未分析完毕");
        }
        Long userId=UserHolder.getUser().getId();;
        InterviewSession session=InterviewSession.builder()
                .userId(userId)
                .difficulty(dto.getDifficulty())
                .category(dto.getCategory())
                .resumeId(dto.getResumeId())
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .title(dto.getTitle())
                .status(0)
                .isDeleted(0)
                .build();
        save(session);
        log.info("面试会话已创建，会话ID:{},用户ID:{}",session.getId(),userId);
        Map<String,Long> map=new HashMap<>();
        map.put("sessionId",session.getId());
        return Result.success(map);
    }
    /**
     *  面试对话：SSE 流式返回 AI 回复
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
        //集合只声明的话为null
         String lastAiQuestion="";
        if(historyMessage!=null&&!historyMessage.isEmpty()){
            Message lastMsg= historyMessage.get(historyMessage.size()-1);
            if(Role.ASSISTANT.getValue().equals(lastMsg.getRole())){
              lastAiQuestion=lastMsg.getContent();
            }
        }
        //lambda表达式里变量必须是final或为被修改过的
       final  String aiQuestion=lastAiQuestion;
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
        ChatMessage chatMessage = saveMessage(sessionId, Role.USER.getValue(), userMessage);
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
                    //异步触发单题评分
                    interviewReportService.saveQuestionReport(
                            sessionId,
                            chatMessage.getId(),
                            aiQuestion,
                            userMessage,
                            standerAnswer);
                    emitter.send("DONE");
                    emitter.complete();
                }

        );
        return emitter;
    }

   public Result finishInterview(Long sessionId) {

       if (sessionId == null) {
           return Result.error("sessionId 不能为空");
       }
       InterviewSession session = getById(sessionId);
       if (session == null) {
           return Result.error("会话不存在");
       }
       if (Integer.valueOf(1).equals(session.getStatus())) {
           return Result.error("面试已结束，请勿重复操作");
       }
       session.setStatus(1);
       session.setUpdateTime(LocalDateTime.now());
       updateById(session);
       log.info("面试结束，sessionId={}", sessionId);
       return Result.success();

   }
   //私有工具方法
    //添加消息
    public ChatMessage saveMessage(Long sessionId,String role,String content){
        ChatMessage chatMessage=ChatMessage.builder()
                .sessionId(sessionId)
                .content(content)
                .role(role)
                .createTime(LocalDateTime.now())
                .build();
        chatMessageService.save(chatMessage);
        return chatMessage;
    }
    //提取简历摘要
    private String getSummary(Long sessionId){
        InterviewSession session = getById(sessionId);
        if(session==null){
            return "简历找不到，请直接开始技术相关的面试";
        }
        Long resumeId = session.getResumeId();
        ResumeAnalysis resumeAnalysis =resumeAnalysisService.getById(resumeId);
        if(resumeAnalysis==null){
            return "简历找不到，请直接开始技术相关的面试";
        }
        String summary=resumeAnalysis.getSummary();
        return summary;
    }

}




