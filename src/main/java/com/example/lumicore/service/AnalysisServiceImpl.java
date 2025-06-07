package com.example.lumicore.service;

import com.example.lumicore.dto.analysis.AnalysisCompleteResponseDto;
import com.example.lumicore.dto.analysis.AnalysisResultDto;
import com.example.lumicore.dto.analysis.ImageAnalysisDto;
import com.example.lumicore.dto.analysis.LandmarkDto;
import com.example.lumicore.dto.question.QuestionItemDto;
import com.example.lumicore.dto.question.QuestionListResponseDto;
import com.example.lumicore.jpa.entity.*;
import com.example.lumicore.jpa.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisServiceImpl implements AnalysisService {

    private final DiaryRepository diaryRepo;
    private final DiaryPhotoRepository photoRepo;
    private final LandmarkRepository landmarkRepo;
    private final DiaryQARepository qaRepo;
    private final AiCallbackProducerService callbackProducerService;

    // 중복 처리 방지를 위한 처리 완료 상태 추적
    private final ConcurrentHashMap<String, Boolean> processedCallbacks = new ConcurrentHashMap<>();

    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    @Transactional
    public QuestionListResponseDto processAnalysis(AnalysisResultDto dto) {
        UUID diaryId = null;

        List<QuestionItemDto> results = new ArrayList<>();
        
        // DTO 유효성 검사
        if (dto == null) {
            throw new IllegalArgumentException("AnalysisResultDto가 null입니다.");
        }
        
        if (dto.getImages() == null || dto.getImages().isEmpty()) {
            throw new IllegalArgumentException("이미지 데이터가 없습니다. images 리스트가 null이거나 비어있습니다.");
        }
        
        log.info("📊 분석 처리 시작: 이미지 {}개, 질문 {}개", 
            dto.getImages().size(), 
            dto.getQuestions() != null ? dto.getQuestions().size() : 0);
        
        // 1) 이미지별 처리
        for (ImageAnalysisDto img : dto.getImages()) {
            String imageId = img.getImageId();
            if (imageId == null || imageId.trim().isEmpty()) {
                log.warn("⚠️ imageId가 null이거나 비어있습니다. 건너뜁니다: {}", img);
                continue;
            }
            
            UUID photoId;
            try {
                photoId = UUID.fromString(imageId);
            } catch (IllegalArgumentException e) {
                log.warn("⚠️ 잘못된 imageId 형식입니다. 건너뜁니다: {}", imageId);
                continue;
            }
            
            DiaryPhoto photo = photoRepo.findById(photoId)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid imageId: " + imageId));

            // 설명·촬영시간 갱신
            photo.updateDescription(img.getDescription());
            photo.updateCapturedAt(LocalDateTime.parse(img.getMetadata().getCaptureDate(), dtf));

            // **위도·경도 갱신** (MetadataDto에 latitude/longitude 필드가 있어야 합니다)
            photo.updateLocation(
                    img.getMetadata().getLocation().getLatitude(),
                    img.getMetadata().getLocation().getLongitude()
            );

            // 랜드마크 매핑
            for (LandmarkDto lmDto : img.getMetadata().getNearbyLandmarks()) {
                Landmark landmark = landmarkRepo.findById(lmDto.getId())
                        .orElseGet(() -> landmarkRepo.save(
                                Landmark.builder()
                                        .id(lmDto.getId())
                                        .name(lmDto.getName())
                                        .build()
                        ));
                photo.addLandmark(landmark);
            }

            photoRepo.save(photo);

            // diaryId 결정
            if (diaryId == null) {
                diaryId = photo.getDiary().getId();
            }
        }

        if (diaryId == null) {
            throw new IllegalArgumentException("처리 가능한 유효한 이미지가 없습니다. 모든 이미지의 imageId가 null이거나 잘못된 형식입니다.");
        }

        UUID finalDiaryId = diaryId;
        Diary diary = diaryRepo.findById(diaryId)
                .orElseThrow(() -> new EntityNotFoundException("Diary not found: " + finalDiaryId));
        diary.updateOverallDaySummary(dto.getOverallDaySummary());
        diaryRepo.save(diary);

        // 2) 질문별 DiaryQA 저장
        if (dto.getQuestions() != null) {
            for (String question : dto.getQuestions()) {
                if (question != null && !question.trim().isEmpty()) {
                    DiaryQA qa = DiaryQA.builder()
                            .diary(Diary.builder().id(diaryId).build())
                            .aiQuestion(question)
                            .userAnswer("")
                            .build();
                    qaRepo.save(qa);

                    results.add(new QuestionItemDto(qa.getId(), question));
                }
            }
        }

        log.info("✅ 분석 처리 완료: diaryId={}, 질문 {}개 저장", diaryId, results.size());

        // 3) 응답 DTO 반환
        return new QuestionListResponseDto(diaryId, results);
    }

    @Override
    @Transactional
    public void handleAnalysisCallback(String diaryId, AnalysisResultDto dto) throws Exception {
        // 중복 처리 방지 체크
        if (processedCallbacks.putIfAbsent(diaryId, true) != null) {
            log.info("🚫 이미 처리된 분석 콜백: diaryId={}", diaryId);
            return;
        }

        try {
            log.info("🎯 분석 콜백 처리 시작: diaryId={}", diaryId);
            
            // 1) 기존 분석 처리 로직 실행 (DB 저장)
            QuestionListResponseDto response = processAnalysis(dto);
            log.info("📊 질문 DB 저장 완료, 총 {}개 질문 생성", response.getQuestions().size());
            
            // 2) 새로운 형식의 응답 데이터 생성
            AnalysisCompleteResponseDto analysisComplete = AnalysisCompleteResponseDto.builder()
                    .overallDaySummary(dto.getOverallDaySummary())
                    .questions(response.getQuestions())
                    .build();
            
            // 3) ANALYSIS_COMPLETE로 한 번에 전송 (기존의 별도 질문 전송 제거)
            log.info("📤 Kafka로 분석 완료 콜백 전송 (JSON 포함)");
            callbackProducerService.sendAnalysisCompleteCallback(diaryId, analysisComplete);
            
            log.info("✅ 분석 콜백 처리 완료: diaryId={}", diaryId);
            
        } catch (Exception e) {
            // 처리 실패 시 상태 제거
            processedCallbacks.remove(diaryId);
            
            log.error("❌ 분석 콜백 처리 중 오류: diaryId={}", diaryId, e);
            
            // 에러 발생 시 에러 콜백 전송
            try {
                callbackProducerService.sendErrorCallback(diaryId, 
                    "분석 처리 중 오류가 발생했습니다: " + e.getMessage(), 
                    "ANALYSIS_SERVICE");
            } catch (Exception callbackError) {
                log.error("에러 콜백 전송 실패: diaryId={}", diaryId, callbackError);
            }
            
            throw e;
        } finally {
            // 처리 완료 후 일정 시간 후 상태 정리 (메모리 누수 방지)
            new Thread(() -> {
                try {
                    Thread.sleep(300000); // 5분 후 정리
                    processedCallbacks.remove(diaryId);
                    log.debug("🧹 처리 완료 상태 정리: diaryId={}", diaryId);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        }
    }
}
