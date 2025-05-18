package com.example.lumicore.controller;

import com.example.lumicore.dto.QuestionListResponseDto;
import com.example.lumicore.dto.analysis.AnalysisResultDto;
import com.example.lumicore.service.AnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
public class AnalysisController {

    private final AnalysisService analysisService;

    @PostMapping
    public ResponseEntity<QuestionListResponseDto> analyze(
            @RequestBody AnalysisResultDto dto) {
        return ResponseEntity.ok(
                analysisService.processAnalysis(dto));
    }
}
