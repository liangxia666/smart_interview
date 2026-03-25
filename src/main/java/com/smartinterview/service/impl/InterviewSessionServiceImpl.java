package com.smartinterview.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;

import com.alibaba.dashscope.common.Role;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartinterview.common.constants.RedisConstants;
import com.smartinterview.common.exception.InterviewSessionException;
import com.smartinterview.common.exception.ResumeAnalysisException;
import com.smartinterview.common.exception.ResumeNotFindException;
import com.smartinterview.common.manager.PromptManager;
import com.smartinterview.common.result.Result;
import com.smartinterview.common.util.UserHolder;
import com.smartinterview.dto.ChatDTO;
import com.smartinterview.dto.StartInterviewDTO;
import com.smartinterview.entity.ChatMessage;
import com.smartinterview.entity.InterviewReport;
import com.smartinterview.entity.InterviewSession;
import com.smartinterview.entity.ResumeAnalysis;
import com.smartinterview.mapper.ChatMessageMapper;
import com.smartinterview.service.*;
import com.smartinterview.mapper.InterviewSessionMapper;
import com.smartinterview.vo.InterviewSessionVO;
import com.smartinterview.vo.InterviewStartVO;
import com.smartinterview.vo.InterviewStatsVO;
import io.reactivex.Flowable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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
    private ChatMessageMapper chatMessageMapper;
    @Autowired
    private AiAnalysisService aianalysisService;
    @Autowired
    private ResumeAnalysisService resumeAnalysisService;
    @Autowired
    private PromptManager promptManager;
    private static final int MAX_HISTORY_MSG=20;
    @Autowired
    private SysQuestionService sysQuestionService;
    @Autowired
    private InterviewReportService interviewReportService;


    public InterviewStartVO startInterview(StartInterviewDTO dto){
        Long userId=UserHolder.getUser().getId();
        ResumeAnalysis resume=resumeAnalysisService.getById(dto.getResumeId());
        if(resume==null){
            throw new ResumeNotFindException("简历未找到，请重新上传简历");
        }
        if(resume.getStatus()==null||resume.getStatus()<3){
            throw new ResumeAnalysisException("简历尚未分析，请稍后重试");
        }
        LambdaQueryWrapper<InterviewSession> existWrapper=new LambdaQueryWrapper<>();
        existWrapper.eq(InterviewSession::getUserId,userId)
                .eq(InterviewSession::getResumeId,resume.getId())
                .eq(InterviewSession::getIsDeleted,0)
                .eq(InterviewSession::getStatus,1)
                .last("limit 1");
        InterviewSession s = getOne(existWrapper);
        if(s!=null){
            return new InterviewStartVO(s.getId());
        }
        InterviewSession session=InterviewSession.builder()
                .userId(userId)
                .difficulty(dto.getDifficulty())
                .jobIntention(dto.getJobIntention())
                .resumeId(dto.getResumeId())
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .title(dto.getTitle())
                .status(1)
                .isDeleted(0)
                .build();
        save(session);
        log.info("面试会话已创建，会话ID:{},用户ID:{}",session.getId(),userId);

        return new InterviewStartVO(session.getId());
    }
    /**
     *  面试对话：SSE 流式返回 AI 回复
     * 由系统提示词+历史会话+当前用户会话共同组成消息集合传到AI生成流式文本
     *
     * @return
     */
    @Override
    public SseEmitter chat(ChatDTO dto) {
        SseEmitter emitter = new SseEmitter(60000L);
        Long sessionId=dto.getSessionId();
        String userMessage=dto.getUserMessage();
        //校验session存在，且未结束
        InterviewSession session=getById(sessionId);
        if(session==null){
            sendErrorAndClose(emitter,"面试会话不存在");
        }
        if(session.getStatus().equals(Integer.valueOf(2))){
            sendErrorAndClose(emitter,"面试已结束，请重新上传简历");
        }
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
        String systemPrompt = promptManager.buildInterviewChatSystemPrompt(
                summaryText,
                standerAnswer,
                session.getDifficulty(),
                session.getJobIntention()
                );
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

   public void finishInterview(Long sessionId) {

       if (sessionId == null) {
           throw new InterviewSessionException("sessionId 不能为空");

       }
       InterviewSession session = getById(sessionId);
       if (session == null) {
           throw new InterviewSessionException("会话不存在");

       }
       if (Integer.valueOf(2).equals(session.getStatus())) {
           throw new InterviewSessionException("面试已结束，请勿重复操作");

       }
       List<InterviewReport> reportList=interviewReportService.lambdaQuery()
                       .eq(InterviewReport::getSessionId,sessionId)
                               .list();
       if(reportList!=null){
          int avgScore=(int) Math.round( reportList.stream()
                  .mapToInt(r->r.getScore()==null?0:r.getScore())//转成int流
                  .average() //求平均数
                  .orElse(0) //集合为空时，没数据置0
          );
          session.setTotalScore(avgScore);
       }

       session.setStatus(2);
       session.setUpdateTime(LocalDateTime.now());
       updateById(session);
       log.info("面试结束，sessionId={}", sessionId);


   }
   public List<InterviewSessionVO> queryInterview(){
        Long userId=UserHolder.getUser().getId();
        LambdaQueryWrapper<InterviewSession> wrapper=new LambdaQueryWrapper<>();
        wrapper.eq(InterviewSession::getUserId,userId)
                .orderByDesc(InterviewSession::getCreateTime);
        List<InterviewSessionVO> vo=list(wrapper).stream()
                .map(r-> BeanUtil.copyProperties(r,InterviewSessionVO.class))
                .collect(Collectors.toList());
        return vo;
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
        chatMessageMapper.insert(chatMessage);
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
    public void sendErrorAndClose(SseEmitter emitter,String message){
        try {
            emitter.send(message);
            emitter.complete();
        } catch (IOException e) {
            emitter.completeWithError(e);
        }
    }
    public void logicalDelete(Long sessionId){
        InterviewSession interviewSession=getById(sessionId);
        if(interviewSession==null){
            throw new InterviewSessionException("找不到该面试记录");
        }
        removeById(sessionId);
    }
    public InterviewStatsVO getInterviewStats(){
        Long userId=UserHolder.getUser().getId();
        List<InterviewSession> list = lambdaQuery().eq(InterviewSession::getUserId, userId)
                .orderByAsc(InterviewSession::getCreateTime)
                .eq(InterviewSession::getStatus, 2)
                .list();
        //统计折线图图数据，每次面试的日期加得分
        List<InterviewStatsVO.ScoreTrend> collect = list.stream().map(r -> {
            InterviewStatsVO.ScoreTrend scoreTrend = new InterviewStatsVO.ScoreTrend();
            scoreTrend.setDate(r.getCreateTime().format(DateTimeFormatter.ofPattern("MM-dd")));
            scoreTrend.setTitle(r.getTitle());
            scoreTrend.setScore(r.getTotalScore());
            return scoreTrend;
        }).collect(Collectors.toList());
        //计算平均分,返回值类型是double需要强转int
        int avgScore=Math.round((int) list.stream().mapToInt(r->r.getTotalScore()).average().orElse(0));
        InterviewStatsVO vo =new InterviewStatsVO();
        vo.setAvgScore(avgScore);
        vo.setTotalCount(list.size());
        vo.setScoreTrends(collect);
        return vo;
    }

}




