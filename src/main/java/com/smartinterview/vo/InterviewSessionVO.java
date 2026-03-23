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
    private String category;      // 面试方向
    private String difficulty;    // 难度
    private Integer status;       // 0进行中 1已结束
    private LocalDateTime createTime;
}
