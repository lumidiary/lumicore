package com.example.lumicore.controller;


import com.example.lumicore.dto.Diary.DiaryResponseDto;
import com.example.lumicore.dto.Diary.DiarySummaryDto;
import com.example.lumicore.dto.question.DiaryAnswerRequestDto;
import com.example.lumicore.service.DiaryService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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
        } catch (EntityNotFoundException e) {
            // 다이어리가 없으면 404
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            // 기타 오류는 500
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    @DeleteMapping("/{diaryId}")
    public ResponseEntity<Void> deleteDiary(@PathVariable UUID diaryId) throws Exception {
        diaryService.deleteDiary(diaryId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<DiarySummaryDto>> getDiariesByUser(@PathVariable UUID userId) throws Exception {
        List<DiarySummaryDto> list = diaryService.getDiariesByUser(userId);
        if (list.isEmpty()) {
            // Exception handling: 404 Not Found
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(list);
    }
}
