package com.example.lumicore.service;

import com.example.lumicore.dto.question.QuestionListResponseDto;
import com.example.lumicore.dto.analysis.AnalysisResultDto;

public interface AnalysisService {
    
    QuestionListResponseDto processAnalysis(AnalysisResultDto dto);
    
    void handleAnalysisCallback(String diaryId, AnalysisResultDto dto) throws Exception;
    
    /** AI 분석 세션 준비 */
    void prepareAnalysisSession(String diaryId);
    
    /** 직접 WebSocket 전송 (레거시 지원) */
    void handleAnalysisCallbackDirect(String diaryId, AnalysisResultDto dto) throws Exception;
}
