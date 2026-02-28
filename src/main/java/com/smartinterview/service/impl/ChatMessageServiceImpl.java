package com.smartinterview.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartinterview.entity.ChatMessage;
import com.smartinterview.service.ChatMessageService;
import com.smartinterview.mapper.ChatMessageMapper;
import org.springframework.stereotype.Service;

/**
* @author 32341
* @description 针对表【chat_message(聊天记录明细表)】的数据库操作Service实现
* @createDate 2026-02-26 16:36:05
*/
@Service
public class ChatMessageServiceImpl extends ServiceImpl<ChatMessageMapper, ChatMessage>
    implements ChatMessageService{

}




