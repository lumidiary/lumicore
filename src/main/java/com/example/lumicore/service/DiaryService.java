package com.example.lumicore.service;

import com.example.lumicore.dto.Diary.DiaryResponseDto;
import com.example.lumicore.dto.question.DiaryAnswerRequestDto;

import java.util.UUID;


public interface DiaryService {

    void submitDiaryAnswers(DiaryAnswerRequestDto requestDto) throws Exception;

    void deleteDiary(UUID diaryId) throws Exception;

    DiaryResponseDto getDiary(UUID diaryId) throws Exception;
}
