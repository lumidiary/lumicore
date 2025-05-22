package com.example.lumicore.websocket;

import com.example.lumicore.service.ImageService;
import com.example.lumicore.service.QueueService;
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

    private final ImageService imageService;
    private final QueueService queueService;
    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;

    // 활성 세션 추적을 위한 맵 (diaryId -> Set<sessionId>)
    private final Map<String, Set<String>> activeSessions = new ConcurrentHashMap<>();
    // 세션 ID와 diaryId 매핑 (sessionId -> diaryId)
    private final Map<String, String> sessionDiaryMapping = new ConcurrentHashMap<>();

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
        
        log.debug("New subscription: sessionId={}, destination={}", sessionId, destination);
        
        if (destination != null && destination.startsWith("/topic/diary/")) {
            String diaryId = extractDiaryIdFromDestination(destination);
            addSession(diaryId, sessionId);
            log.debug("Activated session for diaryId: {}, sessionId: {}", diaryId, sessionId);
        }
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
                }
            }
            log.debug("Removed session mapping - diaryId: {}, sessionId: {}", diaryId, sessionId);
        }
    }

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

    public void sendQuestions(String diaryId, String questions) {
        try {
            WebSocketMessage message = WebSocketMessage.builder()
                .type(MessageType.QUESTION)
                .content(questions)
                .build();
            
            if (sendMessageIfSessionActive("/topic/diary/" + diaryId, message)) {
                log.debug("Question sent to diaryId={}: {}", diaryId, questions);
            } else {
                log.warn("Could not send question to diaryId={} due to no active sessions", diaryId);
            }
        } catch (Exception e) {
            log.error("Failed to send questions via WebSocket", e);
        }
    }

    public void sendAnalysisComplete(String diaryId) {
        try {
            WebSocketMessage message = WebSocketMessage.builder()
                .type(MessageType.ANALYSIS_COMPLETE)
                .build();
            
            if (sendMessageIfSessionActive("/topic/diary/" + diaryId, message)) {
                log.info("Analysis complete message sent for diaryId={}", diaryId);
            } else {
                log.warn("Could not send analysis complete message to diaryId={} due to no active sessions", diaryId);
            }
        } catch (Exception e) {
            log.error("Failed to send analysis complete message", e);
        }
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