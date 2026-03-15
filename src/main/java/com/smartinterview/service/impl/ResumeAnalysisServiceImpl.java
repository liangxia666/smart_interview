package com.smartinterview.service.impl;

import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartinterview.common.exception.ResumeUploadException;
import com.smartinterview.common.result.Result;
import com.smartinterview.common.util.AliOssUtil;
import com.smartinterview.common.util.UserHolder;
import com.smartinterview.config.RabbitConfig;
import com.smartinterview.entity.ResumeAnalysis;
import com.smartinterview.service.AiAnalysisService;
import com.smartinterview.service.ResumeAnalysisService;
import com.smartinterview.mapper.ResumeAnalysisMapper;
import com.smartinterview.vo.ResumeReportVO;
import io.reactivex.Flowable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
* @author 32341
* @description 针对表【resume_analysis(简历智能分析表)】的数据库操作Service实现
* @createDate 2026-02-26 16:36:05
*/
@Service
@Slf4j
public class ResumeAnalysisServiceImpl extends ServiceImpl<ResumeAnalysisMapper, ResumeAnalysis>
    implements ResumeAnalysisService {
    @Autowired
    private AliOssUtil aliOssUtil;
    @Autowired
    private AiAnalysisService aiAnalysisServiceImpl;

    @Autowired
    private RabbitTemplate rabbitTemplate;
    private final String RESUMEID="resumeId";
    public Result upload(MultipartFile file,String intention)  {
        if(!file.getOriginalFilename().toLowerCase().endsWith(".pdf")){
            return Result.error("目前仅支持PDF格式的简历");
        }


        log.info("开始上传简历：{}",file.getOriginalFilename());
        //上传到OSS
        String fileName= null;
        String fileUrl = null;
        try {
            fileName = UUID.randomUUID()+"-"+file.getOriginalFilename();
            fileUrl = aliOssUtil.upload(file.getBytes(), fileName);
        } catch (IOException e) {
            log.info("简历上传失败：{}",e);
            throw new ResumeUploadException("简历上传阿里云失败");
        }
        log.info("上传成功：{}",fileName);
        //初始化数据库
        Long userId = UserHolder.getUser().getId();
        ResumeAnalysis resumeAnalysis=new ResumeAnalysis();
        resumeAnalysis.setUserId(userId);
        resumeAnalysis.setFileUrl(fileUrl);
        resumeAnalysis.setCreateTime(LocalDateTime.now());
        save(resumeAnalysis);
        Long resumeId=resumeAnalysis.getId();
        //发送到mq
        rabbitTemplate.convertAndSend(RabbitConfig.RESUME_PARSE_EXCHANGE,RabbitConfig.RESUME_ROUTING_KEY,resumeId);
        //返回简历id
        Map<String,Long> map=new HashMap<>();
        map.put(RESUMEID,resumeId);

        return Result.success(map);
    }

    /**
     * 简历分析
     * @param resumeId
     * @return
     */
    public SseEmitter streamAiAnalysis(Long resumeId){
        //创建SSEEmitter，封装SSE的连接关闭
        SseEmitter emitter=new SseEmitter(60000L);
        ResumeAnalysis resumeAnalysis=getById(resumeId);
        if(resumeAnalysis==null|| StrUtil.isBlank(resumeAnalysis.getOriginalText())){
            try {
                emitter.send("简历为空，请稍后重试");
                emitter.complete();//关闭连接
            } catch (IOException e) {
                log.error("SSE发送失败",e);
            }
            return emitter;
        }
       //拼接完整的AI回复，用于存到数据库
        StringBuilder fullAiContent=new StringBuilder();
        //调用简历分析方法，一旦有数据生成就返回，用flowable对象接收流式数据类型
        Flowable<GenerationResult> flowable= aiAnalysisServiceImpl.streamAnalyzeResume(resumeAnalysis.getOriginalText());
        //异步订阅，返回给前端
        flowable.subscribe(
                // onNext: 每次 AI 蹦出几个字，就触发这里
                ( GenerationResult result) -> {
                    String chunk=result.getOutput().getChoices().get(0).getMessage().getContent();
                    //拼接新的返回结果
                    fullAiContent.append(chunk);
                    try {
                        if(!fullAiContent.toString().contains("===第二部分")){
                            emitter.send(chunk);
                        }
                    } catch (IOException e) {
                        log.error("客户端断开SSE连接");
                        emitter.completeWithError(e);
                    }

                },
                //发生异常时
                (Throwable err) -> {
                    log.error("大模型流式生成过程中发生错误",err);
                    try {
                        emitter.send("生成异常，请重试");
                        emitter.completeWithError(err);
                    } catch (Exception ignore) {

                    }

                },
                //Ai生成彻底结束时
                ()->{
                    try {
                        log.info("SSE流式输出完毕，总字数：{}",fullAiContent.length());
                        String totalText= fullAiContent.toString();
                        ResumeReportVO resumeReportVO =parseAiResult(totalText);
                        //更新数据库
                        //提取精炼结果
                        String summaryText=getSummary(totalText);
                        String score=getScore(totalText);
                        resumeAnalysis.setStatus(2);
                        resumeAnalysis.setSummary(summaryText);
                        resumeAnalysis.setAiResult(fullAiContent.toString());
                        resumeAnalysis.setScore(score);
                        resumeAnalysis.setUpdateTime(LocalDateTime.now());
                        updateById(resumeAnalysis);
                        //自定义一个发送事件
                        //封装AI生成的结果发送给前端
                        emitter.send(SseEmitter.event()
                                .name("report_finsh")
                                .data(JSONUtil.toJsonStr(resumeReportVO))
                        );
                        emitter.send("[DONE]");
                        emitter.complete();
                    } catch (Exception e) {
                        log.error("SSE 完成更新数据库异常：{}",e);

                    }
                }

        );

        return emitter;
    }
    private String getSummary(String totalText){
        String summaryText="暂无摘要";
        String splitKeyword="【五、项目摘要】";
        //查找字符串在全文中出现的位置
        int summaryIndex=totalText.indexOf(splitKeyword);
        if(summaryIndex!=-1){
            //从指定索引位置截取字符串的剩余部分，并去除首尾空白字符
            summaryText=totalText.substring(summaryIndex).trim();
        }
        return summaryText;
    }
    private String getScore(String totalText){
        String score="暂无得分";
        int start = totalText.indexOf("{\"total\"");
        int end = totalText.lastIndexOf("}");
        if (start != -1 && end != -1) {
           score= totalText.substring(start, end + 1);
        }
        return score;
    }
    public ResumeReportVO parseAiResult(String totalText){
        ResumeReportVO resumeReportVO=new ResumeReportVO();
        resumeReportVO.setUserReport(StrUtil.subBetween(totalText,"===第一部分：展示给候选人的评估报告===", "===第二部分"));
        resumeReportVO.setSort(getScore(totalText));
        resumeReportVO.setSystemSummary(getSummary(totalText));
        return resumeReportVO;

    }


}






