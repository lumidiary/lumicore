package com.example.lumicore.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.context.event.EventListener;
import org.springframework.web.socket.messaging.*;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class DiaryWebSocketHandler {

    private final SimpMessagingTemplate messagingTemplate;

    // 🌟 완전 간소화! diaryId별 단순 boolean
    private final Map<String, Boolean> localActiveSessions = new ConcurrentHashMap<>();
    private final Map<String, Boolean> preparedSessions = new ConcurrentHashMap<>();

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
        
        if (destination != null && destination.startsWith("/topic/diary/")) {
            String diaryId = extractDiaryIdFromDestination(destination);
            
            if (preparedSessions.containsKey(diaryId)) {
                // 🌟 단순하게 true로 설정
                localActiveSessions.put(diaryId, true);
                log.info("✅ 로컬 세션 활성화: diaryId={}", diaryId);
            } else {
                log.warn("❌ 준비되지 않은 세션: diaryId={}", diaryId);
            }
        }
    }

    @EventListener
    public void handleUnsubscribeListener(SessionUnsubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String destination = headerAccessor.getDestination();
        
        if (destination != null && destination.startsWith("/topic/diary/")) {
            String diaryId = extractDiaryIdFromDestination(destination);
            removeLocalSession(diaryId);
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        // unsubscribe에서 처리됨
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        log.debug("WebSocket connection closed: {}", sessionId);
    }

    /**
     * 🌟 세션 준비
     */
    public void prepareSession(String diaryId) {
        preparedSessions.put(diaryId, true);
        log.info("✅ 세션 준비 완료: diaryId={}", diaryId);
    }

    /**
     * 🌟 초간단 로컬 세션 확인
     */
    public boolean hasLocalSession(String diaryId) {
        boolean hasSession = localActiveSessions.getOrDefault(diaryId, false);
        
        if (hasSession) {
            log.debug("✅ 로컬 세션 확인됨: diaryId={}", diaryId);
        }
        
        return hasSession;
    }

    /**
     * 🌟 세션 제거
     */
    private void removeLocalSession(String diaryId) {
        localActiveSessions.remove(diaryId);
        preparedSessions.remove(diaryId);
        log.info("🗑️ 로컬 세션 제거: diaryId={}", diaryId);
    }

    private String extractDiaryIdFromDestination(String destination) {
        return destination.substring("/topic/diary/".length());
    }

    /**
     * 🌟 질문 전송
     */
    public void sendQuestions(String diaryId, String questions) {
        try {
            if (!hasLocalSession(diaryId)) {
                log.debug("👻 현재 Pod에 세션 없음, 무시: diaryId={}", diaryId);
                return;
            }
            
            WebSocketMessage message = WebSocketMessage.builder()
                .type(MessageType.QUESTION)
                .content(questions)
                .build();
            
            messagingTemplate.convertAndSend("/topic/diary/" + diaryId, message);
            log.info("✅ 질문 전송 성공: diaryId={}", diaryId);
            
        } catch (Exception e) {
            log.error("❌ 질문 전송 실패: diaryId={}", diaryId, e);
        }
    }

    /**
     * 🌟 분석 완료 전송
     */
    public void sendAnalysisComplete(String diaryId) {
        try {
            if (!hasLocalSession(diaryId)) {
                log.debug("👻 현재 Pod에 세션 없음, 무시: diaryId={}", diaryId);
                return;
            }
            
            WebSocketMessage message = WebSocketMessage.builder()
                .type(MessageType.ANALYSIS_COMPLETE)
                .build();
            
            messagingTemplate.convertAndSend("/topic/diary/" + diaryId, message);
            log.info("✅ 분석 완료 전송: diaryId={}", diaryId);
            
            // 분석 완료 후 정리
            cleanupSession(diaryId);
            
        } catch (Exception e) {
            log.error("❌ 분석 완료 전송 실패: diaryId={}", diaryId, e);
        }
    }

    /**
     * 🌟 에러 메시지 전송
     */
    public void sendError(String diaryId, String errorMessage) {
        try {
            if (!hasLocalSession(diaryId)) {
                log.debug("👻 현재 Pod에 세션 없음, 무시: diaryId={}", diaryId);
                return;
            }
            
            WebSocketMessage message = WebSocketMessage.builder()
                .type(MessageType.ERROR)
                .content(errorMessage)
                .build();
            
            messagingTemplate.convertAndSend("/topic/diary/" + diaryId, message);
            log.info("⚠️ 에러 메시지 전송: diaryId={}, error={}", diaryId, errorMessage);
            
        } catch (Exception e) {
            log.error("❌ 에러 메시지 전송 실패: diaryId={}", diaryId, e);
        }
    }

    /**
     * 🌟 다이제스트 완료 전송
     */
    public void sendDigestComplete(String diaryId, String digestContent) {
        try {
            if (!hasLocalSession(diaryId)) {
                log.debug("👻 현재 Pod에 세션 없음, 무시: diaryId={}", diaryId);
                return;
            }
            
            WebSocketMessage message = WebSocketMessage.builder()
                .type(MessageType.DIGEST_COMPLETE)
                .content(digestContent)
                .build();
            
            messagingTemplate.convertAndSend("/topic/diary/" + diaryId, message);
            log.info("✅ 다이제스트 완료 전송: diaryId={}", diaryId);
            
        } catch (Exception e) {
            log.error("❌ 다이제스트 완료 전송 실패: diaryId={}", diaryId, e);
        }
    }

    private void cleanupSession(String diaryId) {
        new Thread(() -> {
            try {
                Thread.sleep(5000);
                preparedSessions.remove(diaryId);
                localActiveSessions.remove(diaryId);
                log.debug("🧹 세션 정리 완료: diaryId={}", diaryId);
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
    DIGEST_COMPLETE
} 