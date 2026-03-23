package com.smartinterview.vo;

import lombok.Data;

import java.util.List;

@Data
public class InterviewStatsVO {

    /** 总面试次数 */
    private Integer totalCount;

    /** 平均得分 */
    private Integer avgScore;

    /** 折线图数据点 */
    private List<ScoreTrend> scoreTrends;

    @Data
    public static class ScoreTrend {
        private String date;    // "03-18"，X轴
        private Integer score;  // 得分，Y轴
        private String title;   // 面试标题，悬浮提示
    }
}