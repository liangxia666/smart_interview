package com.smartinterview.vo;

import lombok.Data;
import java.util.List;

@Data
public class InterviewReportVO {
    private Long sessionId;
    private Integer totalScore;    // AVG(score) 取整
    private Integer questionCount; // 总题数
    private Integer correctCount;  // 正确题数
    private String correctRate;    // 正确率（如 "62.5%"）
    private List<QuestionReportItem> items;

    @Data
    public static class QuestionReportItem {
        private String questionText;
        private String userAnswer;
        private Integer score;
        private String comment;
        private Boolean isCorrect;
    }
}
