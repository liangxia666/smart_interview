package com.smartinterview.service;

import com.smartinterview.entity.ChatMessage;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
* @author 32341
* @description 针对表【chat_message(聊天记录明细表)】的数据库操作Service
* @createDate 2026-02-26 16:36:05
*/
public interface ChatMessageService extends IService<ChatMessage> {

    List<ChatMessage> queryBySessionId(Long sessionId);
}
