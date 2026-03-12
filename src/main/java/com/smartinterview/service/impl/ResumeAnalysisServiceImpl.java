package com.smartinterview.service.impl;

import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.StrUtil;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartinterview.common.exception.ResumeUploadException;
import com.smartinterview.common.result.Result;
import com.smartinterview.common.util.AliOssUtil;
import com.smartinterview.common.util.UserHolder;
import com.smartinterview.config.RabbitConfig;
import com.smartinterview.entity.ResumeAnalysis;
import com.smartinterview.service.ResumeAnalysisService;
import com.smartinterview.mapper.ResumeAnalysisMapper;
import io.reactivex.Flowable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
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
    private AiAnalysisService aiAnalysisService;

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
        save(resumeAnalysis);
        Long resumeId=resumeAnalysis.getId();
        //发送到mq
        rabbitTemplate.convertAndSend(RabbitConfig.RESUME_PARSE_EXCHANGE,RabbitConfig.RESUME_ROUTING_KEY,resumeId);
        //返回简历id
        Map<String,Long> map=new HashMap<>();
        map.put(RESUMEID,resumeId);

        return Result.success(map);
    }
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
        //一旦有数据生成就返回，用flowable对象接收流式数据类型
        Flowable<GenerationResult> flowable=aiAnalysisService.streamAnalyzeResume(resumeAnalysis.getOriginalText());
        //异步订阅，返回给前端
        flowable.subscribe(
                // onNext: 每次 AI 蹦出几个字，就触发这里
                ( GenerationResult result) -> {
                    String chunk=result.getOutput().getChoices().get(0).getMessage().getContent();
                    //拼接新的返回结果
                    fullAiContent.append(chunk);
                    try {
                        emitter.send(chunk);
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
                        //更新数据库
                        resumeAnalysis.setStatus(2);
                        resumeAnalysis.setAiResult(fullAiContent.toString());
                        updateById(resumeAnalysis);
                        emitter.send("[DONE]");
                        emitter.complete();
                    } catch (Exception e) {
                        log.error("SSE 完成更新数据库异常：{}",e);

                    }
                }

        );

        return emitter;
    }

}






