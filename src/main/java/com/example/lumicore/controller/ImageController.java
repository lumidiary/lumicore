package com.example.lumicore.controller;

import com.example.lumicore.dto.readSession.ReadSessionResponse;
import com.example.lumicore.dto.uploadSession.UploadParRequest;
import com.example.lumicore.dto.uploadSession.UploadSessionResponse;
import com.example.lumicore.service.ImageService;
import com.example.lumicore.service.QueueService;
import com.example.lumicore.service.AnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/core/ws/images")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Image", description = "이미지 처리 관련 API")
public class ImageController {

    private final ImageService imageService;
    private final QueueService queueService;
    private final AnalysisService analysisService;

    /**
     * POST /api/images/start-session
     * body: { "fileNames": ["a.png","b.jpg", ...] }
     * → diaryId와 PAR 리스트 반환
     */
    @Operation(
            summary = "Write PAR 생성",
            description = "클라이언트에게 파일명 List 요청이 들어오면, diaryId와 Pre-Authenticated Request(PAR) 리스트를 반환합니다."
    )
    @PostMapping("/session")
    public ResponseEntity<UploadSessionResponse> startSession(
            @RequestBody UploadParRequest request) {
        try {
            UploadSessionResponse resp = imageService.startUploadSession(request);
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            log.error("업로드 세션 생성 실패", e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * GET /core/images/session/{diaryId}
     * → ReadSessionResponse DTO를 OCI Queue로 발행하고,
     *    AI 분석 세션을 준비한 뒤 클라이언트에는 HTTP 200 OK 만 돌려줍니다.
     */
    @Operation(
            summary = "READ PAR 생성 & Queue 발행 & AI 분석 세션 준비",
            description = "읽기 Pre-Authenticated Request(PAR) 리스트를 생성한 뒤, OCI Queue로 발행합니다. " +
                    "동시에 AI 분석을 위한 WebSocket 세션을 준비하여 분석 결과를 실시간으로 받을 준비를 합니다. " +
                    "새로운 Kafka 기반 AI 콜백 시스템을 사용합니다. " +
                    "클라이언트에게는 HTTP 200 OK 만 반환합니다."
    )
    @GetMapping("/session/{diaryId}")
    public ResponseEntity<Void> enqueueReadSessionWithAnalysisSession(@PathVariable UUID diaryId) {
        try {
            log.info("🎯 이미지 세션 시작: diaryId={}", diaryId);
            
            // 1. READ PAR 생성
            ReadSessionResponse dto = imageService.generateReadSession(diaryId);
            log.info("📷 READ PAR 생성 완료: {} 개 이미지", dto.getImgPars().size());
            
            // 2. OCI Queue로 발행
            queueService.sendReadSession(dto);
            log.info("📤 OCI Queue 발행 완료");
            
            // 3. AI 분석 세션 준비 (AnalysisService 통합 사용)
            analysisService.prepareAnalysisSession(diaryId.toString());
            log.info("🤖 AI 분석 세션 준비 완료");
            
            log.info("✅ 전체 이미지 세션 준비 완료: diaryId={}", diaryId);
            return ResponseEntity.ok().build();
            
        } catch (IllegalArgumentException e) {
            log.warn("❌ 잘못된 diaryId: {}", diaryId, e);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("❌ 이미지 세션 준비 실패: diaryId={}", diaryId, e);
            return ResponseEntity.status(500).build();
        }
    }
}
