package com.example.lumicore.websocket;

import com.example.lumicore.dto.readSession.ReadSessionResponse;
import com.example.lumicore.service.ImageService;
import com.example.lumicore.service.QueueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.context.event.EventListener;
import org.springframework.web.socket.messaging.*;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class DiaryWebSocketHandler {

    private final SimpMessagingTemplate messagingTemplate;
    private final ImageService imageService;
    private final QueueService queueService;

    // 🌟 간소화: diaryId별 로컬 활성 세션만 관리
    private final Map<String, Boolean> localActiveSessions = new ConcurrentHashMap<>();

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        log.info("New WebSocket connection established: {}", sessionId);
    }

    @EventListener
    public void handleSubscribeListener(SessionSubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String destination = headerAccessor.getDestination();
        System.out.println("[WS DEBUG] SessionSubscribeEvent 호출됨, destination = " + destination);
        
        if (destination != null && destination.startsWith("/topic/diary/")) {
            String diaryId = extractDiaryIdFromDestination(destination);
            
            // 🌟 구독 시 즉시 로컬 세션 활성화
            localActiveSessions.put(diaryId, true);
            System.out.println("[WS DEBUG] localActiveSessions 에 추가됨: " + diaryId);
            log.info("✅ 로컬 세션 활성화: diaryId={}", diaryId);
            
            // 🌟 자동으로 Queue 발행 수행
            try {
                UUID diaryUUID = UUID.fromString(diaryId);
                ReadSessionResponse dto = imageService.generateReadSession(diaryUUID);
                queueService.sendReadSession(dto);
                System.out.println("[WS DEBUG] 자동 Queue 발행 완료: " + diaryId);
                log.info("📤 자동 Queue 발행 완료: diaryId={}, 이미지 수={}", diaryId, dto.getImgPars().size());
            } catch (Exception e) {
                System.out.println("[WS DEBUG] 자동 Queue 발행 실패: " + diaryId + ", error=" + e.getMessage());
                log.error("❌ 자동 Queue 발행 실패: diaryId={}", diaryId, e);
            }
        }
    }

    @EventListener
    public void handleUnsubscribeListener(SessionUnsubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String destination = headerAccessor.getDestination();
        System.out.println("[WS DEBUG] SessionUnsubscribeEvent 호출됨, destination = " + destination);
        
        if (destination != null && destination.startsWith("/topic/diary/")) {
            String diaryId = extractDiaryIdFromDestination(destination);
            removeLocalSession(diaryId);
            System.out.println("[WS DEBUG] 세션 해제: " + diaryId);
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        log.debug("WebSocket connection closed: {}", sessionId);
    }

    /**
     * 🌟 로컬 세션 확인
     */
    public boolean hasLocalSession(String diaryId) {
        boolean hasSession = localActiveSessions.getOrDefault(diaryId, false);
        
        if (hasSession) {
            System.out.println("[WS DEBUG] 로컬 세션 확인됨: " + diaryId);
            log.debug("✅ 로컬 세션 확인됨: diaryId={}", diaryId);
        } else {
            System.out.println("[WS DEBUG] 로컬 세션 없음: " + diaryId);
            log.debug("❌ 로컬 세션 없음: diaryId={}", diaryId);
        }
        
        return hasSession;
    }

    /**
     * 🌟 세션 제거
     */
    private void removeLocalSession(String diaryId) {
        localActiveSessions.remove(diaryId);
        log.info("🗑️ 로컬 세션 제거: diaryId={}", diaryId);
    }

    private String extractDiaryIdFromDestination(String destination) {
        return destination.substring("/topic/diary/".length());
    }

    /**
     * 🌟 질문 전송
     */
    public void sendQuestions(String diaryId, String questions) {
        if (hasLocalSession(diaryId)) {
            WebSocketMessage msg = WebSocketMessage.builder()
                .type(MessageType.QUESTION)
                .content(questions)
                .build();
            System.out.println("[WS DEBUG] sendQuestions() → convertAndSend: /topic/diary/" + diaryId + " : " + msg);
            messagingTemplate.convertAndSend("/topic/diary/" + diaryId, msg);
            log.info("✅ 질문 전송 성공: diaryId={}", diaryId);
        } else {
            System.out.println("[WS DEBUG] sendQuestions() 호출됐으나 세션이 없음: " + diaryId);
        }
    }

    /**
     * 🌟 분석 완료 전송
     */
    public void sendAnalysisComplete(String diaryId) {
        if (hasLocalSession(diaryId)) {
            WebSocketMessage msg = WebSocketMessage.builder()
                .type(MessageType.ANALYSIS_COMPLETE)
                .build();
            System.out.println("[WS DEBUG] sendAnalysisComplete() → convertAndSend: /topic/diary/" + diaryId);
            messagingTemplate.convertAndSend("/topic/diary/" + diaryId, msg);
            log.info("✅ 분석 완료 전송: diaryId={}", diaryId);
            
            // 분석 완료 후 세션 정리
            cleanupSessionDelayed(diaryId);
        } else {
            System.out.println("[WS DEBUG] sendAnalysisComplete() 호출됐으나 세션이 없음: " + diaryId);
        }
    }

    /**
     * 🌟 에러 메시지 전송
     */
    public void sendError(String diaryId, String errorMessage) {
        if (hasLocalSession(diaryId)) {
            WebSocketMessage msg = WebSocketMessage.builder()
                .type(MessageType.ERROR)
                .content(errorMessage)
                .build();
            messagingTemplate.convertAndSend("/topic/diary/" + diaryId, msg);
            log.info("⚠️ 에러 메시지 전송: diaryId={}, error={}", diaryId, errorMessage);
            
            // 에러 발생 시 즉시 세션 정리
            removeLocalSession(diaryId);
        }
    }

    /**
     * 🌟 다이제스트 완료 전송
     */
    public void sendDigestComplete(String diaryId, String digestContent) {
        if (hasLocalSession(diaryId)) {
            WebSocketMessage msg = WebSocketMessage.builder()
                .type(MessageType.DIGEST_COMPLETE)
                .content(digestContent)
                .build();
            messagingTemplate.convertAndSend("/topic/diary/" + diaryId, msg);
            log.info("✅ 다이제스트 완료 전송: diaryId={}", diaryId);
        }
    }

    /**
     * 지연된 세션 정리 (다른 콜백 처리 시간 확보)
     */
    private void cleanupSessionDelayed(String diaryId) {
        new Thread(() -> {
            try {
                Thread.sleep(10000); // 10초 대기
                removeLocalSession(diaryId);
                log.debug("🧹 지연된 세션 정리 완료: diaryId={}", diaryId);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }
}

@lombok.Data
@lombok.Builder
class WebSocketMessage {
    private MessageType type;
    private String content;
}

enum MessageType {
    QUESTION,
    ANALYSIS_COMPLETE,
    ERROR,
    DIGEST_COMPLETE,
    DISCONNECT_REQUEST
} 