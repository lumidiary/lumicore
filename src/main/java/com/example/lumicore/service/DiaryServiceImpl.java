package com.example.lumicore.service;

import com.example.lumicore.dto.diary.DiaryResponseDto;
import com.example.lumicore.dto.diary.DiarySummaryDto;
import com.example.lumicore.dto.diary.PhotoInfoDto;
import com.example.lumicore.dto.diary.QuestionAnswerDiaryDto;
import com.example.lumicore.dto.digest.request.DigestPhotoInfoDto;
import com.example.lumicore.dto.digest.request.DigestRequestEntryDto;
import com.example.lumicore.dto.question.DiaryAnswerRequestDto;
import com.example.lumicore.dto.question.QuestionAnswerDto;
import com.example.lumicore.dto.readSession.ReadParDto;
import com.example.lumicore.dto.readSession.ReadSessionResponse;
import com.example.lumicore.jpa.entity.Diary;
import com.example.lumicore.jpa.entity.DiaryPhoto;
import com.example.lumicore.jpa.entity.DiaryQA;
import com.example.lumicore.jpa.repository.DiaryPhotoRepository;
import com.example.lumicore.jpa.repository.DiaryQARepository;
import com.example.lumicore.jpa.repository.DiaryRepository;
import jakarta.persistence.EntityNotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
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
        List<QuestionAnswerDiaryDto> qaList = diaryQARepository.findByDiaryId(diaryId).stream()
                .map(qa -> QuestionAnswerDiaryDto.builder()
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
        List<PhotoInfoDto> photoList = diaryPhotoRepository.findByDiaryId(diaryId).stream()
                .map(photo -> {
                    ReadParDto rp = urlMap.get(photo.getId());
                    return PhotoInfoDto.builder()
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
                .overallDaySummary(diary.getOverallDaySummary())
                .answers(qaList)
                .photos(photoList)
                .build();
    }

    @Override
    @Transactional
    public void deleteDiary(UUID diaryId) throws Exception {
        // 1) Diary 엔티티 로드
        Diary diary = diaryRepository.findById(diaryId)
                .orElseThrow(() -> new EntityNotFoundException("Diary not found: " + diaryId));

        // 2) Diary에 soft-delete 표시
        diary.markDeleted();

        // 3) 관련 QA들 soft-delete
        diaryQARepository.findByDiaryId(diaryId)
                .forEach(DiaryQA::markDeleted);

        // 4) 관련 Photo들 soft-delete
        diaryPhotoRepository.findByDiaryId(diaryId)
                .forEach(DiaryPhoto::markDeleted);

        // @Transactional 이므로 커밋 시점에 변경사항이 반영됩니다.
    }


    @Override
    @Transactional(readOnly = true)
    public List<DiarySummaryDto> getDiariesByUser(UUID userId) throws Exception {
        // 1) 유저의 다이어리 전체 조회
        List<Diary> diaries = diaryRepository.findByUserId(userId);

        return diaries.stream().map(diary -> {
            UUID dId = diary.getId();

            // 2) 첫 번째 QA 요약
            QuestionAnswerDiaryDto firstAnswer = diaryQARepository.findByDiaryId(dId)
                    .stream().findFirst()
                    .map(qa -> QuestionAnswerDiaryDto.builder()
                            .questionId(qa.getId())
                            .question(qa.getAiQuestion())
                            .answer(qa.getUserAnswer())
                            .build())
                    .orElse(null);

            // 3) 첫 번째 Photo 요약 (엔티티 + READ-PAR URL)
            PhotoInfoDto firstPhoto = diaryPhotoRepository.findByDiaryId(dId)
                    .stream().findFirst()
                    .map(photo -> {
                        // READ-PAR URL 가져오기
                        ReadSessionResponse session = null;
                        try {
                            session = imageService.generateReadSession(dId);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                        ReadParDto par = session.getImgPars().stream()
                                .filter(i -> i.getId().equals(photo.getId()))
                                .findFirst().orElse(null);

                        return PhotoInfoDto.builder()
                                .photoId(photo.getId())
                                .url(par != null ? par.getAccessUri() : null)
                                .latitude(photo.getLatitude())
                                .longitude(photo.getLongitude())
                                .build();
                    })
                    .orElse(null);

            // 4) DiarySummaryDto 조립
            return DiarySummaryDto.builder()
                    .diaryId(dId)
                    .createdAt(diary.getCreatedAt())
                    .firstAnswer(firstAnswer)
                    .firstPhoto(firstPhoto)
                    .build();
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<UUID> getAllUserIds() {
        // Diary 테이블에서 삭제되지 않은(digestEntries 제외) 모든 userId를 distinct 조회
        return diaryRepository.findAll().stream()
                .map(Diary::getUserId)
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public String getUserLocale(UUID userId) {
        // 첫 번째 다이어리에서 locale 추출 (모든 다이어리에 동일하다고 가정)
        return diaryRepository.findByUserId(userId).stream()
                .findFirst()
                .map(Diary::getUserLocale)
                .orElse("ko");
    }

    @Override
    @Transactional(readOnly = true)
    public List<DigestRequestEntryDto> getEntriesForDigest(
            UUID userId,
            LocalDateTime periodStart,
            LocalDateTime periodEnd
    ) {
        // 1) 기간 내 다이어리 조회
        List<Diary> diaries = diaryRepository
                .findByUserIdAndCreatedAtBetween(userId, periodStart, periodEnd);

        // 2) 각 Diary → DigestRequestEntryDto 매핑
        return diaries.stream().map(diary -> {
            // QA 목록
            List<QuestionAnswerDiaryDto> answers = diaryQARepository
                    .findByDiaryId(diary.getId()).stream()
                    .map(qa -> QuestionAnswerDiaryDto.builder()
                            .questionId(qa.getId())
                            .question(qa.getAiQuestion())
                            .answer(qa.getUserAnswer())
                            .build())
                    .collect(Collectors.toList());

            // Photo 매핑
            List<DigestPhotoInfoDto> photos = diaryPhotoRepository
                    .findByDiaryId(diary.getId()).stream()
                    .map(photo -> DigestPhotoInfoDto.builder()
                            .photoId(photo.getId())
                            .description(photo.getDescription())
                            .capturedAt(photo.getCapturedAt())
                            .latitude(photo.getLatitude())
                            .longitude(photo.getLongitude())
                            .build())
                    .collect(Collectors.toList());

            return DigestRequestEntryDto.builder()
                    .diaryId(diary.getId())
                    .createdAt(diary.getCreatedAt())
                    .emotionTag(diary.getEmotion())
                    .overallDaySummary(diary.getOverallDaySummary())
                    .answers(answers)
                    .photos(photos)
                    .build();
        }).collect(Collectors.toList());
    }
}
