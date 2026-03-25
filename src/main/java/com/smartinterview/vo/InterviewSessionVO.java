package com.smartinterview.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterviewSessionVO {
    // SessionListVO
    private Long id;
    private String title;         // 面试标题
    private String difficulty;    // 难度
    private Integer status;       // 0未开始 1进行中 2已结束
    private Integer totalScore;
    private LocalDateTime createTime;
}
