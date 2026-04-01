package com.smartinterview.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumeScoreMessage implements Serializable {
    private Long resumeId;
    private String rawText;
    private String aiResult; // 前台生成的文本
}