package com.smartinterview.entity;

import com.alibaba.dashscope.common.Message;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ChatContext {
    private List<Message> messages;    // 传给大模型的完整消息列表
    private String aiQuestion;         // 本次用户回答的问题（即上一条AI提问）
    private String standerAnswer;      // RAG命中的标准答案
}