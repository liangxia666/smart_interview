package com.smartinterview.service;

import com.smartinterview.entity.InterviewSession;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
* @author 32341
* @description 针对表【interview_session(面试会话主表)】的数据库操作Service
* @createDate 2026-02-26 16:36:05
*/
public interface InterviewSessionService extends IService<InterviewSession> {

    SseEmitter chat(Long sessionId,String userMessage);
}
