package com.smartinterview.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;
import java.util.Date;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @TableName chat_message
 */
@TableName(value ="chat_message")
@Data
@Builder  //自动生成全参构造器，spring不会自动生成无参构造方法
@NoArgsConstructor  //加上无参构造方法生成，builder的有参构造方法被覆盖
@AllArgsConstructor
public class ChatMessage {
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long sessionId;

    private String role;

    private String content;

    private Integer tokenUsage;

    private LocalDateTime createTime;
}