package com.smartinterview.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartinterview.common.exception.InterviewSessionException;
import com.smartinterview.common.util.UserHolder;
import com.smartinterview.entity.ChatMessage;
import com.smartinterview.entity.InterviewSession;
import com.smartinterview.mapper.InterviewSessionMapper;
import com.smartinterview.service.ChatMessageService;
import com.smartinterview.mapper.ChatMessageMapper;
import com.smartinterview.service.InterviewSessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
* @author 32341
* @description 针对表【chat_message(聊天记录明细表)】的数据库操作Service实现
* @createDate 2026-02-26 16:36:05
*/
@Service
public class ChatMessageServiceImpl extends ServiceImpl<ChatMessageMapper, ChatMessage>
    implements ChatMessageService{
    @Autowired
    private InterviewSessionMapper interviewSessionMapper;
    public List<ChatMessage> queryBySessionId(Long sessionId){
        InterviewSession session = interviewSessionMapper.selectById(sessionId);
        if(session==null){
           throw new InterviewSessionException("面试不存在");
       }
        List<ChatMessage> list=lambdaQuery()
                .eq(ChatMessage::getSessionId,sessionId)
                .orderByAsc(ChatMessage::getCreateTime)
                .list();
        return list;
    }

}




