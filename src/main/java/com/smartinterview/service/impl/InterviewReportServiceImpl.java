package com.smartinterview.service.impl;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartinterview.common.result.Result;
import com.smartinterview.entity.InterviewReport;
import com.smartinterview.entity.InterviewSession;
import com.smartinterview.mapper.InterviewReportMapper;
import com.smartinterview.service.AiAnalysisService;
import com.smartinterview.service.InterviewReportService;

import com.smartinterview.service.InterviewSessionService;
import com.smartinterview.vo.InterviewReportVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class InterviewReportServiceImpl extends ServiceImpl<InterviewReportMapper,InterviewReport>
        implements InterviewReportService {
    @Autowired
    private AiAnalysisService aiAnalysisService;
    @Autowired
    private InterviewSessionService interviewSessionService;

    /**
     * 生成报告对象，存到数据库
     * @param sessionId
     * @param messageId
     * @param aiQuestion
     * @param userAnswer
     * @param standardAnswer
     */
    @Async
    public void saveQuestionReport(Long sessionId,Long messageId,String aiQuestion,String userAnswer,String standardAnswer){
        try {
            //调用AI评分
            String aiRaw=aiAnalysisService.evaluateAnswer(aiQuestion,userAnswer,standardAnswer);
            log.info("AI单题评分结果：{}",aiRaw);
            //解析json
            int start=aiRaw.indexOf("{");
            int end=aiRaw.indexOf("}");
            if(start==-1||end==-1){
                log.error("AI评分返回格式异常");
                return ;
            }
            //生成jsonObject对象，键值对格式的json串
            JSONObject json= JSONUtil.parseObj(aiRaw.substring(start,end+1));
            //将报告保存到数据库
            InterviewReport interviewReport = InterviewReport.builder()
                    .sessionId(sessionId)
                    .messageId(messageId)
                    .questionText(aiQuestion)
                    .aiRaw(aiRaw)
                    .userAnswer(userAnswer)
                    .standardAnswer(standardAnswer)
                    .score(json.getInt("score"))
                    .isCorrect(json.getBool("isCorrect"))
                    .comment(json.getStr("comment"))
                    .createTime(LocalDateTime.now())
                    .build();
            save(interviewReport);
            log.info("单体评分已保存,session={},score={}",sessionId,json.getInt("score"));
        } catch (Exception e) {
            log.error("saveQuestionReport 异常,sessionId={}",sessionId,e);
        }


    }

    /**
     * 生成完整报告，封装成VO返回
     * @param sessionId
     * @return
     */
    public Result<InterviewReportVO> buildReport(Long sessionId){
        //判断面试状态
        InterviewSession session=interviewSessionService.getById(sessionId);
        if(session==null){
            return Result.error("面试记录不存在");
        }
        if(session.getStatus().equals(Integer.valueOf(0))){
            return Result.error("面试未结束，请先完成面试");
        }
        //获取报告内容
        LambdaQueryWrapper<InterviewReport> wrapper=new LambdaQueryWrapper<>();
        wrapper.eq(InterviewReport::getSessionId,sessionId)
                .orderByAsc(InterviewReport::getId);
        List<InterviewReport> list=list(wrapper);
        InterviewReportVO interviewReportVO=new InterviewReportVO();
        interviewReportVO.setSessionId(sessionId);
        interviewReportVO.setQuestionCount(list.size());
       //如果查不到数据
        if(list.isEmpty()){
            interviewReportVO.setTotalScore(0);
            interviewReportVO.setCorrectCount(0);
            interviewReportVO.setCorrectRate("0%");
            interviewReportVO.setItems(List.of());
            return Result.success(interviewReportVO);
        }
        int totalScore =(int) Math.round(  //将总分四舍五入
                //将集合元素的score字段转为int 成为intStream
                list.stream().mapToInt(r-> r.getScore()==null?0:r.getScore())
                        .average().orElse(0)); //流为空时设为0
                long correctCount=list.stream()
                        //只保留作对的题的流
                        .filter(r-> Boolean.TRUE.equals(r.getIsCorrect()))
                        .count();//计算数量
       //计算答对比例 80.2% 两个%%输出%
        String correctRate=String.format("%.1f%%",correctCount*100.0/list.size());

        interviewReportVO.setTotalScore(totalScore);
        interviewReportVO.setCorrectRate(correctRate);
        interviewReportVO.setCorrectCount((int)correctCount);
        List<InterviewReportVO.QuestionReportItem> items=list.stream()
                //将list集合中的元素经过转换规则转为另一种元素
                .map(r->{
                    InterviewReportVO.QuestionReportItem item=new InterviewReportVO.QuestionReportItem();
                    item.setQuestionText(r.getQuestionText());
                    item.setComment(r.getComment());
                    item.setIsCorrect(r.getIsCorrect());
                    item.setUserAnswer(r.getUserAnswer());
                    item.setScore(r.getScore());
                    return item;
                }).collect(Collectors.toList());
        interviewReportVO.setItems(items);
        return Result.success(interviewReportVO);
    }


}
