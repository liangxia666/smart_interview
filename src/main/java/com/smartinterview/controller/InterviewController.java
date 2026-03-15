package com.smartinterview.controller;

import com.smartinterview.dto.ChatMsgDTO;
import com.smartinterview.service.impl.InterviewSessionServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@RestController
@RequestMapping("interview")
@Tag(name="模拟面试中心")
public class InterviewController {
    @Autowired
    private InterviewSessionServiceImpl interviewSessionService;
    @Operation(summary="发送聊天消息获取AI流式回复")
    @PostMapping(value="chat",produces = org.springframework.http.MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(@RequestBody ChatMsgDTO chatMsgDTO, HttpServletResponse httpServletResponse){
        //设置服务端将响应的内容转为字节流时的格式
        httpServletResponse.setCharacterEncoding("UTF-8");
        //告知客户端响应体的编码格式
        httpServletResponse.setContentType("text/event-stream;charset=UTF-8");
        return interviewSessionService.chat(chatMsgDTO.getSessionId(), chatMsgDTO.getUserMessage());

    }

}
