package com.example.lumicore.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 2) POST /diaries/{diaryId}/answers
 *    - {qaId, userAnswer} 리스트 받아서 DiaryQA 업데이트
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnswerDiaryRequestDto {
    private List<QaAnswerDto> answers;
}

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
class QaAnswerDto {
    private Long qaId;
    private String userAnswer;
}