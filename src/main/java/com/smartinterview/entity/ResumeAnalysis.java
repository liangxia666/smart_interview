package com.smartinterview.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.time.LocalDateTime;
import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @TableName resume_analysis
 */
@TableName(value ="resume_analysis")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumeAnalysis {
    private Long id;

    private Long userId;

    private String fileUrl;

    private String jobIntention;

    private String originalText;

    private Integer status;

    private String  score;

    private String summary;

    private String aiResult;

    private String suggestion;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDeleted;
}