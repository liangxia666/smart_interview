package com.smartinterview.entity;

import com.baomidou.mybatisplus.annotation.*;

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
    private Integer totalScore;

    //逻辑删除字段0未删除 ，1已删除
    //查询自动加where isDelete=0,移除时自动加isDelete=1
    @TableLogic
    private Integer isDeleted;
}