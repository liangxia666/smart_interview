package com.smartinterview.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionImportDTO {
    private String category;
    private String question;
    private String answer;
    private Integer difficulty;
}
