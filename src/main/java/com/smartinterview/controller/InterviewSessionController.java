package com.smartinterview.controller;

import com.smartinterview.common.result.Result;
import com.smartinterview.dto.StartInterviewDTO;
import com.smartinterview.service.InterviewSessionService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

/**
 * 面试会话管理 Controller
 * 负责面试的整个生命周期：开始 → 对话 → 结束
 */
@RestController
@RequestMapping("/interview/session")
@Slf4j
public class InterviewSessionController {

    @Autowired
    private InterviewSessionService interviewSessionService;

    /**
     * 开始面试
     * 前端进入面试页时调用，创建 InterviewSession 记录，返回 sessionId
     *
     * POST /interview/session/start
     *
     * 请求体示例：
     * {
     *   "resumeId": 1,
     *   "category": "Java",
     *   "difficulty": "medium",
     *   "title": "Java 后端面试"
     * }
     *
     * 响应示例：
     * { "code": 200, "data": { "sessionId": 1001 } }
     */
    @PostMapping("/start")
    public Result startInterview(@RequestBody StartInterviewDTO dto) {
        return interviewSessionService.startInterview(dto);
    }

    /**
     * 面试对话（SSE 流式）
     * 每次用户发送消息时调用，AI 以流式方式逐字返回回复
     *
     * POST /interview/session/chat?sessionId=1001&userMessage=你好
     *
     * 响应：text/event-stream
     * 每个 chunk 是 AI 回复的片段，最后一条为 "DONE" 表示本轮结束
     */
    @Operation(summary="发送聊天消息获取AI流式回复")
    @PostMapping(value="chat",produces = org.springframework.http.MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(@RequestParam Long sessionId,
                           @RequestParam String userMessage,
                           HttpServletResponse httpServletResponse){
        //设置服务端将响应的内容转为字节流时的格式
        httpServletResponse.setCharacterEncoding("UTF-8");
        //告知客户端响应体的编码格式
        httpServletResponse.setContentType("text/event-stream;charset=UTF-8");
        return interviewSessionService.chat(sessionId,userMessage);

    }

    /**
     * 结束面试
     * 用户点击「结束面试」按钮时调用，将 session.status 置为 1
     * 结束后才可以调用报告接口查看结果
     *
     * POST /interview/session/finish?sessionId=1001
     *
     * 响应示例：
     * { "code": 200, "msg": "success", "data": null }
     */
    @PostMapping("session/finish") //方法路径加/匹配剧决对路径
    public Result finishInterview(@RequestParam Long sessionId){
        log.info("结束面试:",sessionId);
        return interviewSessionService.finishInterview(sessionId);
    }
}
