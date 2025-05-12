package com.example.lumicore.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 외부에서 받아오는 AI 질문
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestionDto {
    private Long qaId;
    private String aiQuestion;
}