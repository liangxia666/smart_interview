package com.smartinterview.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.smartinterview.common.constants.RabbitConstants;
import com.smartinterview.common.exception.ResumeAnalysisException;
import com.smartinterview.common.exception.ResumeNotFindException;
import com.smartinterview.common.exception.ResumeUploadException;
import com.smartinterview.common.manager.ResumeStateManager;
import com.smartinterview.common.result.PageResult;
import com.smartinterview.common.util.AliOssUtil;
import com.smartinterview.common.util.UserHolder;
import com.smartinterview.config.RabbitConfig;
import com.smartinterview.entity.ResumeScoreMessage;
import com.smartinterview.entity.ResumeAnalysis;

import com.smartinterview.entity.ResumeScoreMessage;
import com.smartinterview.service.AiAnalysisService;
import com.smartinterview.service.ResumeAnalysisService;
import com.smartinterview.mapper.ResumeAnalysisMapper;


import com.smartinterview.vo.ResumeDetailVO;
import com.smartinterview.vo.ResumeUploadVO;
import com.smartinterview.vo.ResumeVO;
import io.reactivex.Flowable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

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
    //创建代理对象时，注入接口的bean,
    // JDK 代理是基于接口造替身，替身只认接口，不认你的实现类！
//    @Lazy //延迟注入，在第一次使用时注入
//    @Autowired
//    private ResumeAnalysisService self;
    @Autowired
    private ResumeStateManager resumeStateManager;


    public ResumeUploadVO upload(MultipartFile file)  {
        if(!file.getOriginalFilename().toLowerCase().endsWith(".pdf")){
            throw new ResumeUploadException("目前仅支持pdf格式的文件上传");
        }
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
        resumeAnalysis.setName(file.getOriginalFilename());
        resumeAnalysis.setFileUrl(fileUrl);
        resumeAnalysis.setCreateTime(LocalDateTime.now());
        resumeAnalysis.setUpdateTime(LocalDateTime.now());
        resumeAnalysis.setStatus(Integer.valueOf(0));
        save(resumeAnalysis);
        Long resumeId=resumeAnalysis.getId();
        //发送到mq
        rabbitTemplate.convertAndSend(RabbitConstants.RESUME_PARSE_EXCHANGE,RabbitConstants.RESUME_PARSE_ROUTING_KEY,resumeId);


        return new ResumeUploadVO(resumeId);
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
            safeSendAndClose(emitter, "简历为空，请稍后重试");
            return emitter;
        }
        // 缓存原始文本
        final String rawText = resumeAnalysis.getOriginalText();
       //拼接完整的AI回复，用于存到数据库
        StringBuilder fullAiContent=new StringBuilder();
        //调用简历分析方法，一旦有数据生成就返回，用flowable对象接收流式数据类型
        Flowable<GenerationResult> flowable= aiAnalysisService.streamAnalyzeResume(resumeAnalysis.getOriginalText());
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
                    log.error("简历流式分析异常",err);
                    safeSendAndClose(emitter, "生成异常，请重试");
                },
                //Ai生成彻底结束时  onComplete 里任何 throw 都会变成 UndeliverableException
                ()->{
                    safeSendAndComplete(emitter, "[DONE]");
                    log.info("前台轨完成，resumeId={}，总字数={}", resumeId, fullAiContent.length());

                        //将resume状态改为2
                        resumeStateManager.updateToTextGenerated(resumeId,fullAiContent.toString());
                        //创建MQ消息
                        ResumeScoreMessage mqMessage=ResumeScoreMessage.builder()
                                        .resumeId(resumeId)
                                        .rawText(rawText)
                                        .aiResult(fullAiContent.toString())
                                        .build();
                        //发送到消息队列
                        rabbitTemplate.convertAndSend(RabbitConstants.RESUME_SCORE_EXCHANGE,RabbitConstants.RESUME_SCORE_ROUTING_KEY,mqMessage);
                }
        );
        return emitter;
    }


    private void safeSendAndClose(SseEmitter emitter, String msg) {
        try {
            emitter.send(msg);
            emitter.complete();
        } catch (Exception ignore) {}
    }

    private void safeSendAndComplete(SseEmitter emitter, String msg) {
        try {
            emitter.send(msg);
            emitter.complete();
        } catch (Exception e) {
            emitter.completeWithError(e);
        }
    }
    //前端每两秒查询一个简历状态，status，时打分完成，简历分析成功
    public ResumeDetailVO getResumeDetail(Long resumeId){
        ResumeAnalysis resume=getById(resumeId);
        if(resume==null){
            throw new ResumeNotFindException("简历不存在");
        }
        ResumeDetailVO vo=new ResumeDetailVO();
        vo.setStatus(resume.getStatus());
        //当状态为3时评分完成返回评分
        if(resume.getStatus().equals(Integer.valueOf(3))){
            vo.setScore(resume.getScore());
        }
        return vo;
    }
    public String queryReport(Long resumeId){
        ResumeAnalysis resume = getById(resumeId);
        if(resume.getStatus()<3){
            throw new ResumeAnalysisException("简历尚未处理完成");
        }
        return resume.getAiResult();

    }



//    public PageResult pageQuery(Integer current, Integer size){
//        IPage<ResumeAnalysis> page=new Page<>(current,size);
//        Long userId=UserHolder.getUser().getId();
//        IPage<ResumeAnalysis> pageResult = lambdaQuery()
//                .eq(ResumeAnalysis::getUserId, userId)
//                .orderByDesc(ResumeAnalysis::getCreateTime)
//                .page(page);
//        List<ResumeAnalysis> resumeAnalysisList=pageResult.getRecords();
//        List<ResumePageVO> list= resumeAnalysisList.stream().map(
//                r->{
//                    ResumePageVO resumePageVO = BeanUtil.copyProperties(r, ResumePageVO.class);
//                    return resumePageVO;
//                }
//        ).collect(Collectors.toList());
//
//     return new PageResult(pageResult.getTotal(),pageResult.getPages(),current,pageResult.getSize(),list);
//    }
    public List<ResumeVO> queryResume(){
        Long userId=UserHolder.getUser().getId();
        LambdaQueryWrapper<ResumeAnalysis> wrapper=new LambdaQueryWrapper<>();
        wrapper.eq(ResumeAnalysis::getUserId,userId)
                .orderByDesc(ResumeAnalysis::getCreateTime);
         List<ResumeVO> list=list(wrapper).stream()
                 //加上{} return也必须加上；
                 .map(r->{return BeanUtil.copyProperties(r,ResumeVO.class);
                 }).collect(Collectors.toList());
         return list;
    }

    @Override
    public void logicalDelete(Long resumeId) {
        ResumeAnalysis resume=getById(resumeId);
        if(resume==null){
            throw new ResumeNotFindException("简历不存在");
        }
        removeById(resumeId);//update is_delete=1
    }
}






