package com.example.lumicore.controller;


import com.example.lumicore.dto.question.DiaryAnswerRequestDto;
import com.example.lumicore.service.DiaryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/diaries")
@RequiredArgsConstructor
public class DiaryController {

    private final DiaryService diaryService;


    @PostMapping("/answers")
    public ResponseEntity<Void> submitDiaryAnswers(
            @RequestBody DiaryAnswerRequestDto requestDto
    ) {
        diaryService.submitDiaryAnswers(requestDto);
        return ResponseEntity.ok().build();
    }



}
