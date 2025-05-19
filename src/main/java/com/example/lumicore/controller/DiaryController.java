package com.example.lumicore.controller;


import com.example.lumicore.dto.Diary.DiaryResponseDto;
import com.example.lumicore.dto.question.DiaryAnswerRequestDto;
import com.example.lumicore.service.DiaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/diaries")
@RequiredArgsConstructor
public class DiaryController {

    private final DiaryService diaryService;


    @PostMapping("/answers")
    public ResponseEntity<Void> submitDiaryAnswers(
            @RequestBody DiaryAnswerRequestDto requestDto
    ) throws Exception {
        diaryService.submitDiaryAnswers(requestDto);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{diaryId}")
    public ResponseEntity<DiaryResponseDto> getDiary(@PathVariable UUID diaryId) {
        try {
            DiaryResponseDto dto = diaryService.getDiary(diaryId);
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

}
