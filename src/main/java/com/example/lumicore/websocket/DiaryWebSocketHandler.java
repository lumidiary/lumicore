package com.example.lumicore.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.context.event.EventListener;
import org.springframework.web.socket.messaging.*;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

@Slf4j
@Component
@RequiredArgsConstructor
public class DiaryWebSocketHandler {

    private final SimpMessagingTemplate messagingTemplate;

    // 활성 세션 추적을 위한 맵 (diaryId -> Set<sessionId>)
    private final Map<String, Set<String>> activeSessions = new ConcurrentHashMap<>();
    // 세션 ID와 diaryId 매핑 (sessionId -> diaryId)
    private final Map<String, String> sessionDiaryMapping = new ConcurrentHashMap<>();
    // 준비된 세션들 (diaryId -> 준비 상태)
    private final Map<String, Boolean> preparedSessions = new ConcurrentHashMap<>();

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        log.debug("New WebSocket connection established: {}", sessionId);
    }

    @EventListener
    public void handleSubscribeListener(SessionSubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String destination = headerAccessor.getDestination();
        String sessionId = headerAccessor.getSessionId();
        
        log.info("=== 구독 이벤트 ===");
        log.info("SessionId: {}", sessionId);
        log.info("Destination: {}", destination);
        log.info("현재 준비된 세션들: {}", preparedSessions.keySet());
        
        // 기존 패턴 유지: /topic/diary/{diaryId}
        if (destination != null && destination.startsWith("/topic/diary/")) {
            String diaryId = extractDiaryIdFromDestination(destination);
            log.info("추출된 DiaryId: {}", diaryId);
            
            // 준비된 세션인지 확인
            if (preparedSessions.containsKey(diaryId)) {
                addSession(diaryId, sessionId);
                log.info("세션 활성화 성공: diaryId={}, sessionId={}", diaryId, sessionId);
            } else {
                log.warn("준비되지 않은 세션: diaryId={}, sessionId={}", diaryId, sessionId);
                log.warn("현재 준비된 세션들: {}", preparedSessions.keySet());
            }
        } else {
            log.warn("잘못된 destination 패턴: {}", destination);
        }
        
        log.info("현재 활성 세션들: {}", activeSessions);
        log.info("=== 구독 이벤트 끝 ===");
    }

    @EventListener
    public void handleUnsubscribeListener(SessionUnsubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        
        log.debug("Unsubscription: sessionId={}", sessionId);
        removeSession(sessionId);
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        log.debug("WebSocket connection closed: {}", sessionId);
        
        removeSession(sessionId);
    }

    /**
     * HTTP 요청 시 세션 준비
     */
    public void prepareSession(String diaryId) {
        preparedSessions.put(diaryId, true);
        log.info("세션 준비 완료: diaryId={}", diaryId);
        log.info("현재 준비된 세션들: {}", preparedSessions.keySet());
    }

    private synchronized void addSession(String diaryId, String sessionId) {
        // 이전 세션 매핑 제거
        removeSession(sessionId);
        
        // 새로운 세션 매핑 추가
        activeSessions.computeIfAbsent(diaryId, k -> new ConcurrentSkipListSet<>()).add(sessionId);
        sessionDiaryMapping.put(sessionId, diaryId);
        
        log.debug("Added session mapping - diaryId: {}, sessionId: {}", diaryId, sessionId);
    }

    private synchronized void removeSession(String sessionId) {
        String diaryId = sessionDiaryMapping.remove(sessionId);
        if (diaryId != null) {
            Set<String> sessions = activeSessions.get(diaryId);
            if (sessions != null) {
                sessions.remove(sessionId);
                if (sessions.isEmpty()) {
                    activeSessions.remove(diaryId);
                    // 세션이 모두 제거되면 준비 상태도 해제
                    preparedSessions.remove(diaryId);
                }
            }
            log.debug("Removed session mapping - diaryId: {}, sessionId: {}", diaryId, sessionId);
        }
    }

    //수정: 올바른 destination 패턴
    private String extractDiaryIdFromDestination(String destination) {
        return destination.substring("/topic/diary/".length());
    }

    private boolean isSessionActive(String diaryId) {
        Set<String> sessions = activeSessions.get(diaryId);
        boolean isActive = sessions != null && !sessions.isEmpty();
        log.debug("Checking session status for diaryId: {} - isActive: {}, active sessions: {}", 
                 diaryId, isActive, sessions);
        return isActive;
    }

    private boolean sendMessageIfSessionActive(String destination, WebSocketMessage message) {
        String diaryId = extractDiaryIdFromDestination(destination);
        if (!isSessionActive(diaryId)) {
            log.debug("No active sessions for diaryId: {}. Message will not be sent.", diaryId);
            return false;
        }

        try {
            messagingTemplate.convertAndSend(destination, message);
            log.debug("Message sent successfully to diaryId: {}", diaryId);
            return true;
        } catch (Exception e) {
            log.error("Failed to send message to diaryId: {}", diaryId, e);
            return false;
        }
    }

    // 수정: 기존 패턴 사용
    public void sendQuestions(String diaryId, String questions) {
        try {
            log.info("질문 전송 시도: diaryId={}, question={}", diaryId, questions);
            
            WebSocketMessage message = WebSocketMessage.builder()
                .type(MessageType.QUESTION)
                .content(questions)
                .build();
            
            // 세션 상태 확인 로그
            boolean sessionExists = isSessionActive(diaryId);
            log.info("세션 활성 상태: diaryId={}, isActive={}", diaryId, sessionExists);
            
            if (sendMessageIfSessionActive("/topic/diary/" + diaryId, message)) {
                log.info("질문 전송 성공: diaryId={}", diaryId);
            } else {
                log.warn("활성 세션 없음: diaryId={}", diaryId);
                log.warn("현재 활성 세션들: {}", activeSessions.keySet());
            }
        } catch (Exception e) {
            log.error("질문 전송 실패", e);
        }
    }

    // 수정: 기존 패턴 사용
    public void sendAnalysisComplete(String diaryId) {
        try {
            WebSocketMessage message = WebSocketMessage.builder()
                .type(MessageType.ANALYSIS_COMPLETE)
                .build();
            
            if (sendMessageIfSessionActive("/topic/diary/" + diaryId, message)) {
                log.info("Analysis complete message sent for diaryId={}", diaryId);
                // 분석 완료 후 세션 정리
                cleanupSession(diaryId);
            } else {
                log.warn("Could not send analysis complete message to diaryId={} due to no active sessions", diaryId);
            }
        } catch (Exception e) {
            log.error("Failed to send analysis complete message", e);
        }
    }

    /**
     * 분석 완료 후 세션 정리
     */
    private void cleanupSession(String diaryId) {
        // 일정 시간 후 세션 정리
        new Thread(() -> {
            try {
                Thread.sleep(5000); // 5초 후 정리
                preparedSessions.remove(diaryId);
                activeSessions.remove(diaryId);
                log.debug("Session cleaned up for diaryId: {}", diaryId);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Session cleanup interrupted for diaryId: {}", diaryId);
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
    ANALYSIS_COMPLETE
} 