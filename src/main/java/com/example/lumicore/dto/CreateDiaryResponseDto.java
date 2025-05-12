package com.example.lumicore.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 1) POST /diaries 응답
 *    - diaryId, 방금 생성된 QA 질문 리스트 반환
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateDiaryResponseDto {
    private Long diaryId;
    private List<QuestionDto> questions;
}