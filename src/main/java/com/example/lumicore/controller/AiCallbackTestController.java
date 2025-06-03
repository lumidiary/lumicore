package com.example.lumicore.controller;

import com.example.lumicore.service.AiCallbackProducerService;
import com.example.lumicore.service.AnalysisService;
import com.example.lumicore.websocket.DiaryWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/core/test/ai-callback")
@RequiredArgsConstructor
public class AiCallbackTestController {

    private final AiCallbackProducerService callbackProducerService;
    private final DiaryWebSocketHandler webSocketHandler;
    private final AnalysisService analysisService;

    /**
     * WebSocket 세션 준비 (테스트용) - 직접 WebSocket 핸들러 사용
     */
    @PostMapping("/prepare-session/{diaryId}")
    public ResponseEntity<String> prepareSession(@PathVariable String diaryId) {
        try {
            webSocketHandler.prepareSession(diaryId);
            log.info("✅ 테스트 세션 준비 완료: diaryId={}", diaryId);
            return ResponseEntity.ok("Session prepared for diaryId: " + diaryId);
        } catch (Exception e) {
            log.error("❌ 세션 준비 실패: diaryId={}", diaryId, e);
            return ResponseEntity.badRequest().body("Session preparation failed: " + e.getMessage());
        }
    }

    /**
     * Analysis 서비스를 통한 세션 준비 (권장)
     */
    @PostMapping("/prepare-analysis-session/{diaryId}")
    public ResponseEntity<String> prepareAnalysisSession(@PathVariable String diaryId) {
        try {
            analysisService.prepareAnalysisSession(diaryId);
            log.info("✅ Analysis 세션 준비 완료: diaryId={}", diaryId);
            return ResponseEntity.ok("Analysis session prepared for diaryId: " + diaryId);
        } catch (Exception e) {
            log.error("❌ Analysis 세션 준비 실패: diaryId={}", diaryId, e);
            return ResponseEntity.badRequest().body("Analysis session preparation failed: " + e.getMessage());
        }
    }

    /**
     * 질문 생성 완료 콜백 테스트
     */
    @PostMapping("/send-question/{diaryId}")
    public ResponseEntity<String> sendQuestionCallback(
            @PathVariable String diaryId,
            @RequestBody Map<String, String> request) {
        try {
            String questions = request.getOrDefault("questions", "테스트 질문입니다.");
            callbackProducerService.sendQuestionCallback(diaryId, questions);
            log.info("✅ 질문 콜백 전송 완료: diaryId={}", diaryId);
            return ResponseEntity.ok("Question callback sent for diaryId: " + diaryId);
        } catch (Exception e) {
            log.error("❌ 질문 콜백 전송 실패: diaryId={}", diaryId, e);
            return ResponseEntity.badRequest().body("Callback send failed: " + e.getMessage());
        }
    }

    /**
     * 분석 완료 콜백 테스트
     */
    @PostMapping("/send-analysis-complete/{diaryId}")
    public ResponseEntity<String> sendAnalysisCompleteCallback(@PathVariable String diaryId) {
        try {
            callbackProducerService.sendAnalysisCompleteCallback(diaryId);
            log.info("✅ 분석 완료 콜백 전송 완료: diaryId={}", diaryId);
            return ResponseEntity.ok("Analysis complete callback sent for diaryId: " + diaryId);
        } catch (Exception e) {
            log.error("❌ 분석 완료 콜백 전송 실패: diaryId={}", diaryId, e);
            return ResponseEntity.badRequest().body("Callback send failed: " + e.getMessage());
        }
    }

    /**
     * 다이제스트 완료 콜백 테스트
     */
    @PostMapping("/send-digest-complete/{diaryId}")
    public ResponseEntity<String> sendDigestCompleteCallback(
            @PathVariable String diaryId,
            @RequestBody Map<String, String> request) {
        try {
            String digestContent = request.getOrDefault("content", "테스트 다이제스트 내용입니다.");
            callbackProducerService.sendDigestCompleteCallback(diaryId, digestContent);
            log.info("✅ 다이제스트 완료 콜백 전송 완료: diaryId={}", diaryId);
            return ResponseEntity.ok("Digest complete callback sent for diaryId: " + diaryId);
        } catch (Exception e) {
            log.error("❌ 다이제스트 완료 콜백 전송 실패: diaryId={}", diaryId, e);
            return ResponseEntity.badRequest().body("Callback send failed: " + e.getMessage());
        }
    }

    /**
     * 에러 콜백 테스트
     */
    @PostMapping("/send-error/{diaryId}")
    public ResponseEntity<String> sendErrorCallback(
            @PathVariable String diaryId,
            @RequestBody Map<String, String> request) {
        try {
            String errorMessage = request.getOrDefault("error", "테스트 에러 메시지입니다.");
            String serviceType = request.getOrDefault("serviceType", "TEST_SERVICE");
            callbackProducerService.sendErrorCallback(diaryId, errorMessage, serviceType);
            log.info("✅ 에러 콜백 전송 완료: diaryId={}", diaryId);
            return ResponseEntity.ok("Error callback sent for diaryId: " + diaryId);
        } catch (Exception e) {
            log.error("❌ 에러 콜백 전송 실패: diaryId={}", diaryId, e);
            return ResponseEntity.badRequest().body("Callback send failed: " + e.getMessage());
        }
    }

    /**
     * 현재 세션 상태 확인
     */
    @GetMapping("/session-status/{diaryId}")
    public ResponseEntity<Map<String, Object>> getSessionStatus(@PathVariable String diaryId) {
        boolean hasSession = webSocketHandler.hasLocalSession(diaryId);
        Map<String, Object> status = Map.of(
            "diaryId", diaryId,
            "hasLocalSession", hasSession,
            "timestamp", System.currentTimeMillis()
        );
        return ResponseEntity.ok(status);
    }
} 