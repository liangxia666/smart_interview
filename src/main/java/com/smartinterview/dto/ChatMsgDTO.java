package com.smartinterview.dto;

import lombok.Data;

@Data
public class ChatMsgDTO {
    private Long sessionId;
    private String userMessage;
}
