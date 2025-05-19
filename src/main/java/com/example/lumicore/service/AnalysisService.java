package com.example.lumicore.service;

import com.example.lumicore.dto.question.QuestionListResponseDto;
import com.example.lumicore.dto.analysis.AnalysisResultDto;

public interface AnalysisService {
    QuestionListResponseDto processAnalysis(AnalysisResultDto dto);
}
