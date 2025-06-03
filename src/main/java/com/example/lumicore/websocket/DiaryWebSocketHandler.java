package com.example.lumicore.websocket;

import com.example.lumicore.service.AiCallbackProducerService;
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
    private final AiCallbackProducerService callbackProducerService;

    // 🌟 완전 간소화! diaryId별 단순 boolean
    private final Map<String, Boolean> localActiveSessions = new ConcurrentHashMap<>();
    private final Map<String, Boolean> preparedSessions = new ConcurrentHashMap<>();

    // 🌟 새로 추가: 대기 중인 구독을 저장할 맵
    private final Map<String, Boolean> pendingSubscriptions = new ConcurrentHashMap<>();

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
            boolean isPrepared = preparedSessions.containsKey(diaryId);
            System.out.println("[WS DEBUG] preparedSessions.containsKey(" + diaryId + ") = " + isPrepared);
            
            if (isPrepared) {
                // 🌟 이미 준비된 세션이면 즉시 활성화
                localActiveSessions.put(diaryId, true);
                System.out.println("[WS DEBUG] localActiveSessions 에 추가됨: " + diaryId);
                log.info("✅ 로컬 세션 활성화: diaryId={}", diaryId);
            } else {
                // 🌟 NEW: 세션이 준비되지 않았다면 즉시 준비
                System.out.println("[WS DEBUG] 세션이 준비되지 않음, 자동으로 준비 시작: " + diaryId);
                log.info("🚀 자동 세션 준비 시작: diaryId={}", diaryId);
                
                // 즉시 세션 준비 (브로드캐스트 + 로컬 준비)
                prepareSessionAutomatic(diaryId);
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
        // unsubscribe에서 처리됨
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        log.debug("WebSocket connection closed: {}", sessionId);
    }

    /**
     * 🌟 세션 준비 - 대기 중인 구독도 활성화
     */
    public void prepareSession(String diaryId) {
        System.out.println("[WS DEBUG] prepareSession() 호출됨: " + diaryId);
        
        // 세션 준비 상태로 설정
        preparedSessions.put(diaryId, true);
        
        // 🌟 이미 대기 중인 구독이 있다면 활성화
        if (pendingSubscriptions.containsKey(diaryId)) {
            localActiveSessions.put(diaryId, true);
            pendingSubscriptions.remove(diaryId);
            System.out.println("[WS DEBUG] 대기 중인 구독을 활성화함: " + diaryId);
            log.info("✅ 대기 중인 구독 활성화: diaryId={}", diaryId);
        } else {
            // 대기 중인 구독이 없어도 일단 활성화 상태로 설정
            localActiveSessions.put(diaryId, true);
            System.out.println("[WS DEBUG] 새로운 세션 활성화: " + diaryId);
        }
        
        log.info("✅ 세션 준비 및 활성화 완료: diaryId={}", diaryId);
    }

    /**
     * 🌟 로컬 세션 확인 (대기 중인 세션도 포함)
     */
    public boolean hasLocalSession(String diaryId) {
        boolean hasActiveSession = localActiveSessions.getOrDefault(diaryId, false);
        boolean hasPendingSession = pendingSubscriptions.getOrDefault(diaryId, false);
        boolean hasSession = hasActiveSession || hasPendingSession;
        
        if (hasSession) {
            System.out.println("[WS DEBUG] 로컬 세션 확인됨 (active=" + hasActiveSession + ", pending=" + hasPendingSession + "): " + diaryId);
            log.debug("✅ 로컬 세션 확인됨: diaryId={}", diaryId);
        } else {
            System.out.println("[WS DEBUG] 로컬 세션 없음: " + diaryId);
            log.debug("❌ 로컬 세션 없음: diaryId={}", diaryId);
        }
        
        return hasSession;
    }

    /**
     * 🌟 세션 제거 (모든 상태 정리)
     */
    private void removeLocalSession(String diaryId) {
        localActiveSessions.remove(diaryId);
        preparedSessions.remove(diaryId);
        pendingSubscriptions.remove(diaryId); // 🌟 대기 상태도 제거
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
            
            // 분석 완료 후 정리
            cleanupSession(diaryId);
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
            
            // 에러 발생 시에도 세션 정리
            cleanupSessionImmediate(diaryId);
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

    private void cleanupSession(String diaryId) {
        new Thread(() -> {
            try {
                Thread.sleep(5000);
                preparedSessions.remove(diaryId);
                localActiveSessions.remove(diaryId);
                pendingSubscriptions.remove(diaryId); // 🌟 대기 상태도 정리
                log.debug("🧹 세션 정리 완료: diaryId={}", diaryId);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    /**
     * 🚀 자동 세션 준비 (구독 시점에 호출)
     */
    private void prepareSessionAutomatic(String diaryId) {
        try {
            // 1. 브로드캐스트 먼저 (다른 파드들도 준비하도록)
            if (callbackProducerService != null) {
                callbackProducerService.sendSessionPrepareBroadcast(diaryId);
                log.info("📢 자동 세션 준비 브로드캐스트 전송: diaryId={}", diaryId);
            }
            
            // 2. 로컬 세션 즉시 준비
            preparedSessions.put(diaryId, true);
            localActiveSessions.put(diaryId, true);
            
            System.out.println("[WS DEBUG] 자동 세션 준비 완료: " + diaryId);
            log.info("✅ 자동 세션 준비 완료: diaryId={}", diaryId);
            
        } catch (Exception e) {
            log.error("❌ 자동 세션 준비 실패: diaryId={}", diaryId, e);
            
            // 실패해도 일단 대기 상태로는 저장
            pendingSubscriptions.put(diaryId, true);
            log.info("⏰ 자동 준비 실패, 대기 상태로 저장: diaryId={}", diaryId);
        }
    }

    /**
     * 🌟 질문 생성 완료 후 정리 추가
     */
    public void sendQuestionsComplete(String diaryId, String questions) {
        if (hasLocalSession(diaryId)) {
            WebSocketMessage msg = WebSocketMessage.builder()
                .type(MessageType.QUESTION)
                .content(questions)
                .build();
            System.out.println("[WS DEBUG] sendQuestionsComplete() → convertAndSend: /topic/diary/" + diaryId + " : " + msg);
            messagingTemplate.convertAndSend("/topic/diary/" + diaryId, msg);
            log.info("✅ 질문 전송 성공: diaryId={}", diaryId);
            
            // 질문 전송 후 세션 정리 (질문만 필요한 경우)
            cleanupSessionImmediate(diaryId);
        } else {
            System.out.println("[WS DEBUG] sendQuestionsComplete() 호출됐으나 세션이 없음: " + diaryId);
        }
    }

    /**
     * 🌟 즉시 세션 정리 (지연 없이)
     */
    private void cleanupSessionImmediate(String diaryId) {
        preparedSessions.remove(diaryId);
        localActiveSessions.remove(diaryId);
        pendingSubscriptions.remove(diaryId);
        log.info("🧹 즉시 세션 정리 완료: diaryId={}", diaryId);
    }

    /**
     * 🌟 질문 전송 후 연결 종료 요청
     */
    public void sendQuestionsAndRequestDisconnect(String diaryId, String questions) {
        if (hasLocalSession(diaryId)) {
            // 1. 질문 먼저 전송
            WebSocketMessage questionMsg = WebSocketMessage.builder()
                .type(MessageType.QUESTION)
                .content(questions)
                .build();
            messagingTemplate.convertAndSend("/topic/diary/" + diaryId, questionMsg);
            log.info("✅ 질문 전송 성공: diaryId={}", diaryId);
            
            // 2. 잠시 후 연결 종료 요청
            new Thread(() -> {
                try {
                    Thread.sleep(2000); // 2초 대기 (클라이언트가 질문을 읽을 시간)
                    
                    WebSocketMessage disconnectMsg = WebSocketMessage.builder()
                        .type(MessageType.DISCONNECT_REQUEST)
                        .content("질문 생성이 완료되었습니다. 연결을 정리합니다.")
                        .build();
                    messagingTemplate.convertAndSend("/topic/diary/" + diaryId, disconnectMsg);
                    log.info("🔌 연결 종료 요청 전송: diaryId={}", diaryId);
                    
                    // 서버 측 세션도 정리
                    Thread.sleep(3000); // 클라이언트가 정리할 시간
                    cleanupSessionImmediate(diaryId);
                    
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        }
    }

    /**
     * 세션 준비 상태로 마킹 (브로드캐스트 메시지 처리용)
     */
    public void markSessionPrepared(String diaryId) {
        preparedSessions.put(diaryId, true);
        log.info("세션 준비 완료로 마킹: diaryId={}", diaryId);
    }

    public void sendQuestionToClient(String diaryId, String content) {
        Map<String, Object> message = Map.of(
            "type", "QUESTION",
            "content", content
        );
        messagingTemplate.convertAndSend("/topic/diary/" + diaryId, message);
        log.info("📝 질문 전송: diaryId={}", diaryId);
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