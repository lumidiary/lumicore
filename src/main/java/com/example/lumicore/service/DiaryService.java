package com.example.lumicore.service;

import com.example.lumicore.dto.question.DiaryAnswerRequestDto;


public interface DiaryService {

    void submitDiaryAnswers(DiaryAnswerRequestDto requestDto);
}
