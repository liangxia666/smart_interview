package com.smartinterview.controller;

import com.smartinterview.common.result.Result;
import com.smartinterview.entity.ChatMessage;
import com.smartinterview.service.ChatMessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Slf4j
@Tag(name="聊天消息管理模块")
@RequestMapping("chat/message")
public class ChatMessageController {
    @Autowired
    private ChatMessageService chatMessageService;
    @Operation(summary="查询聊天消息")
    @GetMapping("list")
    public Result queryChat(@PathVariable Long sessionId){
     List<ChatMessage> list= chatMessageService.queryBySessionId(sessionId);
     return Result.success(list);
    }
}
