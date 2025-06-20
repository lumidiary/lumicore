package com.example.lumicore.service;

import com.example.lumicore.dto.diary.DiaryResponseDto;
import com.example.lumicore.dto.diary.DiarySummaryDto;
import com.example.lumicore.dto.diary.PhotoInfoDto;
import com.example.lumicore.dto.diary.QuestionAnswerDiaryDto;
import com.example.lumicore.dto.digest.request.DigestPhotoInfoDto;
import com.example.lumicore.dto.digest.request.DigestRequestEntryDto;
import com.example.lumicore.dto.question.DiaryAnswerRequestDto;
import com.example.lumicore.dto.question.QuestionAnswerDto;
import com.example.lumicore.dto.readSession.ReadSessionResponse;
import com.example.lumicore.dto.readSession.ImageData;
import com.example.lumicore.dto.digest.request.DigestQuestionDto;
import com.example.lumicore.jpa.entity.Diary;
import com.example.lumicore.jpa.entity.DiaryPhoto;
import com.example.lumicore.jpa.entity.DiaryQA;
import com.example.lumicore.jpa.entity.EmotionTag;
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
import java.util.stream.IntStream;

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
        
        // String → EmotionTag 변환 with 예외 처리
        try {
            EmotionTag emotionTag = EmotionTag.valueOf(dto.getEmotionTag().trim().toUpperCase());
            diary.updateEmotionTag(emotionTag);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid EmotionTag value: {}, defaulting to NEUTRAL", dto.getEmotionTag());
            diary.updateEmotionTag(EmotionTag.NEUTRAL);
        }
        
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
        Map<String, ImageData> urlMap = session.getImages().stream()
                .collect(Collectors.toMap(ImageData::getId, Function.identity()));

        // 4) DiaryPhoto → PhotoInfo DTO 변환
        List<PhotoInfoDto> photoList = diaryPhotoRepository.findByDiaryId(diaryId).stream()
                .map(photo -> {
                    ImageData imageData = urlMap.get(photo.getId().toString());
                    return PhotoInfoDto.builder()
                            .photoId(photo.getId())
                            .url(imageData != null ? imageData.getUrl() : null)
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
                .overallDaySummary(diary.getOverallDaySummary())
                .createdAt(diary.getCreatedAt())
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
        // 1) 유저 다이어리 전체 조회
        List<Diary> diaries = diaryRepository.findByUserId(userId);

        return diaries.stream()
                // ① deletedAt != null 인 것은 결과에서 제외
                .filter(diary -> diary.getDeletedAt() == null)
                .map(diary -> {
                    UUID dId = diary.getId();

                    // ② overallDaySummary 조회
                    String overallDaySummary = diary.getOverallDaySummary();

                    // ③ 첫 번째 Photo 요약 (엔티티 + READ-PAR URL)
                    PhotoInfoDto firstPhoto = diaryPhotoRepository.findByDiaryId(dId)
                            .stream().findFirst()
                            .map(photo -> {
                                ReadSessionResponse session;
                                try {
                                    // imageService 를 통해 ReadSession 생성
                                    session = imageService.generateReadSession(dId);
                                } catch (Exception e) {
                                    throw new RuntimeException(e);
                                }
                                ImageData imageData = session.getImages().stream()
                                        .filter(i -> i.getId().equals(photo.getId().toString()))
                                        .findFirst().orElse(null);

                                return PhotoInfoDto.builder()
                                        .photoId(photo.getId())
                                        .url(imageData != null ? imageData.getUrl() : null)
                                        .latitude(photo.getLatitude())
                                        .longitude(photo.getLongitude())
                                        .build();
                            })
                            .orElse(null);

                    // ④ DiarySummaryDto 조립
                    return DiarySummaryDto.builder()
                            .diaryId(dId)
                            .createdAt(diary.getCreatedAt())
                            .overallDaySummary(overallDaySummary)
                            .emotionTag(diary.getEmotion().name())
                            .firstPhoto(firstPhoto)
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<UUID> getAllUserIds() {
        // Diary 테이블에서 삭제되지 않은(digestEntries 제외) 모든 userId를 distinct 조회
        return diaryRepository.findAll().stream()
                .filter(diary -> diary.getDeletedAt() == null) // 삭제 안된 것만
                .map(Diary::getUserId)
                .filter(userId -> userId != null) // userId가 null이 아닌 것만
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
            // QA 목록 - DigestQuestionDto 사용, index를 1부터 순서대로
            List<DiaryQA> qaList = diaryQARepository.findByDiaryId(diary.getId());
            List<DigestQuestionDto> questions = IntStream.range(0, qaList.size())
                    .<DigestQuestionDto>mapToObj(i -> {
                        DiaryQA qa = qaList.get(i);
                        return DigestQuestionDto.builder()
                                .index(i + 1)
                                .question(qa.getAiQuestion())
                                .answer(qa.getUserAnswer())
                                .build();
                    })
                    .collect(Collectors.toList());

            // Photo 매핑 - index를 1부터 순서대로, capturedAt 제거
            List<DiaryPhoto> photoList = diaryPhotoRepository.findByDiaryId(diary.getId());
            List<DigestPhotoInfoDto> imageDescriptions = IntStream.range(0, photoList.size())
                    .<DigestPhotoInfoDto>mapToObj(i -> {
                        DiaryPhoto photo = photoList.get(i);
                        return DigestPhotoInfoDto.builder()
                                .index(i + 1)
                                .description(photo.getDescription())
                                .latitude(photo.getLatitude())
                                .longitude(photo.getLongitude())
                                .build();
                    })
                    .collect(Collectors.toList());

            return DigestRequestEntryDto.builder()
                    .id(diary.getId().toString())  // diaryId → id, String 타입으로
                    .date(diary.getCreatedAt())     // createdAt → date
                    .emotion(diary.getEmotion().name())  // emotionTag → emotion, String 타입으로
                    .imageDescriptions(imageDescriptions)  // photos → imageDescriptions
                    .overallDaySummary(diary.getOverallDaySummary())
                    .questions(questions)  // answers → questions, DigestQuestionDto 사용
                    .build();
        }).collect(Collectors.toList());
    }
}
