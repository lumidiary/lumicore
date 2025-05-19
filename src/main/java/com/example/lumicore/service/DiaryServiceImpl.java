package com.example.lumicore.service;

import com.example.lumicore.dto.Diary.DiaryResponseDto;
import com.example.lumicore.dto.Diary.PhotoInfo;
import com.example.lumicore.dto.Diary.QuestionAnswer;
import com.example.lumicore.dto.question.DiaryAnswerRequestDto;
import com.example.lumicore.dto.question.QuestionAnswerDto;
import com.example.lumicore.dto.readSession.ReadParDto;
import com.example.lumicore.dto.readSession.ReadSessionResponse;
import com.example.lumicore.jpa.entity.Diary;
import com.example.lumicore.jpa.entity.DiaryQA;
import com.example.lumicore.jpa.repository.DiaryPhotoRepository;
import com.example.lumicore.jpa.repository.DiaryQARepository;
import com.example.lumicore.jpa.repository.DiaryRepository;
import jakarta.persistence.EntityNotFoundException;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DiaryServiceImpl implements DiaryService {

    private final DiaryRepository diaryRepository;
    private final DiaryPhotoRepository diaryPhotoRepository;
    private final DiaryQARepository diaryQARepository;
    private final ImageService imageService;


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


    @Override
    @Transactional(readOnly = true)
    public DiaryResponseDto getDiary(UUID diaryId) throws Exception {
        // 1) Diary 기본정보 조회
        Diary diary = diaryRepository.findById(diaryId)
                .orElseThrow(() -> new EntityNotFoundException("Diary not found: " + diaryId));

        // 2) QA → QuestionAnswer DTO 변환
        List<QuestionAnswer> qaList = diaryQARepository.findByDiaryId(diaryId).stream()
                .map(qa -> QuestionAnswer.builder()
                        .questionId(qa.getId())
                        .question(qa.getAiQuestion())
                        .answer(qa.getUserAnswer())
                        .build()
                )
                .collect(Collectors.toList());

        // 3) READ-PAR 세션 생성 & URL 맵 구성
        ReadSessionResponse session = imageService.generateReadSession(diaryId);
        Map<UUID, ReadParDto> urlMap = session.getImgPars().stream()
                .collect(Collectors.toMap(ReadParDto::getId, Function.identity()));

        // 4) DiaryPhoto → PhotoInfo DTO 변환 (위도/경도 + accessUri)
        List<PhotoInfo> photoList = diaryPhotoRepository.findByDiaryId(diaryId).stream()
                .map(photo -> {
                    ReadParDto rp = urlMap.get(photo.getId());
                    return PhotoInfo.builder()
                            .photoId(photo.getId())
                            .url(rp != null ? rp.getAccessUri() : null)
                            .latitude(photo.getLatitude())
                            .longitude(photo.getLongitude())
                            .build();
                })
                .collect(Collectors.toList());

        // 5) 최종 DiaryResponseDto 반환
        return DiaryResponseDto.builder()
                .diaryId(diary.getId())
                .userId(diary.getUserId())
                .userLocale(diary.getUserLocale())
                .emotionTag(diary.getEmotion().name())
                .createdAt(diary.getCreatedAt())
                .answers(qaList)
                .photos(photoList)
                .build();
    }
}
