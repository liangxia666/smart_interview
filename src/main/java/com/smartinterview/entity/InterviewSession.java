package com.smartinterview.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;
import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @TableName interview_session
 */
@TableName(value ="interview_session")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewSession {
    private Long id;

    private Long userId;

    private Long resumeId;

    private Integer status;

    private String category;

    private String difficulty;

    private String title;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Integer isDeleted;
}