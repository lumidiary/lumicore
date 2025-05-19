package com.example.lumicore.service;

import com.example.lumicore.dto.question.DiaryAnswerRequestDto;
import com.example.lumicore.dto.question.QuestionAnswerDto;
import com.example.lumicore.jpa.entity.Diary;
import com.example.lumicore.jpa.entity.DiaryQA;
import com.example.lumicore.jpa.repository.DiaryQARepository;
import com.example.lumicore.jpa.repository.DiaryRepository;
import jakarta.persistence.EntityNotFoundException;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DiaryServiceImpl implements DiaryService {

    private final DiaryRepository diaryRepository;
    private final DiaryQARepository diaryQARepository;


    @Override
    @Transactional
    public void submitDiaryAnswers(DiaryAnswerRequestDto dto){
        UUID diaryId = dto.getDiaryId();

        // 1) 기존 Diary 조회
        Diary diary = diaryRepository.findById(diaryId)
                .orElseThrow(() -> new EntityNotFoundException("Diary not found: " + diaryId));

        // 2) userId, emotionTag 업데이트
        diary.updateUserId(dto.getUserId());
        diary.updateEmotionTag(dto.getEmotionTag());
        diaryRepository.save(diary);

        // 3) 기존 DiaryQA 레코드를 찾아서 userAnswer만 채워넣기
        for (QuestionAnswerDto qa : dto.getAnswers()) {
            UUID questionId = qa.getId();

            DiaryQA diaryQA = diaryQARepository.findById(questionId)
                    .orElseThrow(() -> new EntityNotFoundException("DiaryQA not found: " + questionId));

            diaryQA.updateUserAnswer(qa.getAnswer());
            diaryQARepository.save(diaryQA);
        }
    }
}
