package com.example.lumicore.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 질문 + 답변 포함 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QaDto {
    private Long qaId;
    private String aiQuestion;
    private String userAnswer;
    private LocalDateTime answeredAt;
}
