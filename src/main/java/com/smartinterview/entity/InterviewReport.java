package com.smartinterview.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@TableName("interview_question_report")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewReport {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long sessionId;
    private Long messageId;
    private String questionText;
    private String userAnswer;
    private String standardAnswer;
    private Integer score;
    private String comment;
    private Boolean isCorrect;
    private String aiRaw;
    private LocalDateTime createTime;
}
