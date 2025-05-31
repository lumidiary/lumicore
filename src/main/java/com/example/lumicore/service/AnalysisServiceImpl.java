package com.example.lumicore.service;

import com.example.lumicore.dto.question.QuestionItemDto;
import com.example.lumicore.dto.question.QuestionListResponseDto;
import com.example.lumicore.dto.analysis.AnalysisResultDto;
import com.example.lumicore.dto.analysis.ImageAnalysisDto;
import com.example.lumicore.dto.analysis.LandmarkDto;
import com.example.lumicore.jpa.entity.Diary;
import com.example.lumicore.jpa.entity.DiaryPhoto;
import com.example.lumicore.jpa.entity.Landmark;
import com.example.lumicore.jpa.entity.DiaryQA;
import com.example.lumicore.jpa.repository.DiaryPhotoRepository;
import com.example.lumicore.jpa.repository.DiaryRepository;
import com.example.lumicore.jpa.repository.LandmarkRepository;
import com.example.lumicore.jpa.repository.DiaryQARepository;
import com.example.lumicore.websocket.DiaryWebSocketHandler;
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
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisServiceImpl implements AnalysisService {

    private final DiaryRepository diaryRepo;
    private final DiaryPhotoRepository photoRepo;
    private final LandmarkRepository landmarkRepo;
    private final DiaryQARepository qaRepo;
    private final DiaryWebSocketHandler webSocketHandler;
    private final AiCallbackProducerService callbackProducerService;

    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * AI 분석 세션을 준비합니다.
     * 분석 시작 전에 WebSocket 세션을 준비하여 콜백을 받을 수 있도록 합니다.
     */
    public void prepareAnalysisSession(String diaryId) {
        webSocketHandler.prepareSession(diaryId);
        log.info("🎯 분석 세션 준비 완료: diaryId={}", diaryId);
    }

    @Override
    @Transactional
    public QuestionListResponseDto processAnalysis(AnalysisResultDto dto) {
        UUID diaryId = null;

        List<QuestionItemDto> results = new ArrayList<>();
        // 1) 이미지별 처리
        for (ImageAnalysisDto img : dto.getImages()) {
            UUID photoId = UUID.fromString(img.getImageId());
            DiaryPhoto photo = photoRepo.findById(photoId)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid imageId: " + img.getImageId()));

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


        UUID finalDiaryId = diaryId;
        Diary diary = diaryRepo.findById(diaryId)
                .orElseThrow(() -> new EntityNotFoundException("Diary not found: " + finalDiaryId));
        diary.updateOverallDaySummary(dto.getOverallDaySummary());
        diaryRepo.save(diary);


        // 2) 질문별 DiaryQA 저장
        for (String question : dto.getQuestions()) {
            DiaryQA qa = DiaryQA.builder()
                    .diary(Diary.builder().id(diaryId).build())
                    .aiQuestion(question)
                    .userAnswer("")
                    .build();
            qaRepo.save(qa);

            results.add(new QuestionItemDto(qa.getId(), question));
        }

        // 3) 응답 DTO 반환
        return new QuestionListResponseDto(diaryId, results);
    }

    @Override
    @Transactional
    public void handleAnalysisCallback(String diaryId, AnalysisResultDto dto) throws Exception {
        try {
            log.info("🎯 분석 콜백 처리 시작: diaryId={}", diaryId);
            
            // 1) 기존 분석 처리 로직 실행 (DB 저장)
            QuestionListResponseDto response = processAnalysis(dto);
            log.info("📊 질문 DB 저장 완료, 총 {}개 질문 생성", response.getQuestions().size());
            
            // 2) Kafka를 통해 질문 콜백 전송 (모든 Pod에 브로드캐스팅)
            String allQuestions = response.getQuestions().stream()
                    .map(QuestionItemDto::getQuestion)
                    .collect(Collectors.joining("\n"));
            
            log.info("📤 Kafka로 질문 콜백 전송 시작");
            callbackProducerService.sendQuestionCallback(diaryId, allQuestions);
            
            // 3) 잠시 대기 후 분석 완료 콜백 전송
            new Thread(() -> {
                try {
                    Thread.sleep(1000); // 1초 대기
                    log.info("📤 Kafka로 분석 완료 콜백 전송");
                    callbackProducerService.sendAnalysisCompleteCallback(diaryId);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.error("분석 완료 콜백 전송 중 인터럽트: diaryId={}", diaryId);
                }
            }).start();
            
            log.info("✅ 분석 콜백 처리 완료: diaryId={}", diaryId);
            
        } catch (Exception e) {
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
        }
    }

    /**
     * 직접 WebSocket 전송 (레거시 지원)
     * 기존 방식과의 호환성을 위해 유지
     */
    @Transactional
    public void handleAnalysisCallbackDirect(String diaryId, AnalysisResultDto dto) throws Exception {
        try {
            log.info("🔄 직접 WebSocket 분석 콜백 처리 시작: diaryId={}", diaryId);
            
            // 기존 분석 처리 로직 실행
            QuestionListResponseDto response = processAnalysis(dto);
            
            log.info("📨 직접 WebSocket 전송 시작, 질문 수: {}", response.getQuestions().size());
            
            // 생성된 각 질문을 WebSocket을 통해 클라이언트에게 직접 전송
            for (QuestionItemDto question : response.getQuestions()) {
                try {
                    log.debug("질문 전송: {}", question.getQuestion());
                    Thread.sleep(100); // 각 메시지 사이에 약간의 딜레이
                    webSocketHandler.sendQuestions(diaryId, question.getQuestion());
                } catch (Exception e) {
                    log.error("질문 전송 실패 - diaryId: {}, question: {}", 
                        diaryId, question.getQuestion(), e);
                }
            }
            
            // 잠시 대기 후 분석 완료 메시지 전송
            Thread.sleep(500);
            log.info("📨 직접 분석 완료 메시지 전송");
            webSocketHandler.sendAnalysisComplete(diaryId);
            
            log.info("✅ 직접 WebSocket 분석 콜백 처리 완료: diaryId={}", diaryId);
            
        } catch (Exception e) {
            log.error("❌ 직접 WebSocket 분석 콜백 처리 중 오류: diaryId={}", diaryId, e);
            
            // 에러 발생 시 직접 에러 메시지 전송
            try {
                webSocketHandler.sendError(diaryId, "분석 처리 중 오류가 발생했습니다: " + e.getMessage());
            } catch (Exception wsError) {
                log.error("WebSocket 에러 메시지 전송 실패: diaryId={}", diaryId, wsError);
            }
            
            throw e;
        }
    }
}
