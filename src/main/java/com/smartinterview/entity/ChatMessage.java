package com.smartinterview.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * @TableName chat_message
 */
@TableName(value ="chat_message")
@Data
public class ChatMessage {
    private Long id;

    private Long sessionId;

    private String role;

    private String content;

    private Integer tokenUsage;

    private Date createTime;
}