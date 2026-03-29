package com.smartinterview.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class QuestionVector {
        Long id;
        String question;
        String answer;
        float[] vector;
    }