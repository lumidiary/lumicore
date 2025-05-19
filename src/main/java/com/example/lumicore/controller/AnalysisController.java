package com.example.lumicore.controller;

import com.example.lumicore.dto.question.QuestionListResponseDto;
import com.example.lumicore.dto.analysis.AnalysisResultDto;
import com.example.lumicore.service.AnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor

@Tag(name = "Analysis", description = "질문 처리 관련 API")
public class AnalysisController {

    private final AnalysisService analysisService;

    @Operation(
            summary = "질문 처리",
            description = "반환된 이미지 분석 결과 및 질문을 처리하고 질문들의 리스트를 다이어리 ID와 함깨 결과를 반환합니다."
    )
    @PostMapping
    public ResponseEntity<QuestionListResponseDto> analyze(
            @RequestBody AnalysisResultDto dto) {
        return ResponseEntity.ok(
                analysisService.processAnalysis(dto));
    }
}
