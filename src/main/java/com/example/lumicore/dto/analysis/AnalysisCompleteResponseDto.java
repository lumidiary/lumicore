package com.example.lumicore.dto.analysis;

import com.example.lumicore.dto.question.QuestionItemDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisCompleteResponseDto {
    private String overallDaySummary;
    private List<QuestionItemDto> questions;
}