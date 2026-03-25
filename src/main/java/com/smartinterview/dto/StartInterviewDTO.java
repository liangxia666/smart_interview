package com.smartinterview.dto;

import lombok.Data;

@Data
public class StartInterviewDTO {
    private String title;
    private String difficulty;
    private String jobIntention;
    private Long resumeId;
}
