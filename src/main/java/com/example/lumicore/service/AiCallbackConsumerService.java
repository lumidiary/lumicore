package com.example.lumicore.service;

import com.example.lumicore.dto.question.QuestionListResponseDto;
import com.example.lumicore.websocket.DiaryWebSocketHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import static com.example.lumicore.service.AiCallbackProducerService.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiCallbackConsumerService {

    private final DiaryWebSocketHandler webSocketHandler;
    private final ObjectMapper objectMapper;
    private final AiCallbackDataService callbackDataService;

    @Value("${app.kafka.message.ttl-minutes:5}")
    private int messageTtlMinutes;

    /**
     * AI 서비스 콜백 메시지를 처리합니다.
     * OCI Streaming(Kafka)에서 수신한 콜백을 모든 pod에서 받아서
     * 해당 diaryId에 대한 WebSocket 세션이 있는지 확인하고 처리합니다.
     */
    @KafkaListener(
        topics = "${app.kafka.topic.ai-callback:ai-callback}",
        groupId = "${spring.kafka.consumer.group-id}",
        concurrency = "3"
    )
    public void handleAiCallback(
            @Payload String message,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {
        
        try {
            // JSON 파싱
            var payload = objectMapper.readTree(message);
            String diaryId = payload.get("diaryId").asText();
            String callbackType = payload.get("callbackType").asText();
            long messageTimestamp = payload.get("timestamp").asLong();
            
            // TTL 체크 - 오래된 메시지 필터링
            if (isMessageExpired(messageTimestamp)) {
                log.debug("⏰ TTL 만료 메시지 무시 - DiaryId: {}, Type: {}, Age: {}분", 
                    diaryId, callbackType, getMessageAgeMinutes(messageTimestamp));
                acknowledgment.acknowledge();
                return;
            }
            
            // 세션 체크 - 로컬 세션이 있는 경우만 처리
            if (!webSocketHandler.hasLocalSession(diaryId)) {
                log.debug("👻 로컬 세션 없음 - DiaryId: {}, Type: {}", diaryId, callbackType);
                acknowledgment.acknowledge();
                return;
            }
            
            // 실제 처리되는 메시지만 INFO 레벨로 로그
            log.info("🎯 AI 콜백 처리 - DiaryId: {}, Type: {}", diaryId, callbackType);
            
            // 콜백 타입별 처리
            switch (callbackType) {
                case CALLBACK_TYPE_QUESTION -> {
                    var content = payload.get("data").get("content").asText();
                    webSocketHandler.sendQuestions(diaryId, content);
                    log.info("📝 질문 전송 완료 - DiaryId: {}", diaryId);
                }
                case CALLBACK_TYPE_ANALYSIS_COMPLETE -> {
                    webSocketHandler.sendAnalysisComplete(diaryId);
                    log.info("✅ 분석 완료 알림 전송 - DiaryId: {}", diaryId);
                }
                case CALLBACK_TYPE_DIGEST_COMPLETE -> {
                    var digestContent = payload.has("data") && payload.get("data").has("content") 
                        ? payload.get("data").get("content").asText() 
                        : "다이제스트 생성이 완료되었습니다.";
                    webSocketHandler.sendDigestComplete(diaryId, digestContent);
                    log.info("📊 다이제스트 완료 알림 전송 - DiaryId: {}", diaryId);
                }
                case CALLBACK_TYPE_ERROR -> {
                    var errorContent = payload.get("data").get("content").asText();
                    webSocketHandler.sendError(diaryId, errorContent);
                    log.warn("❌ 에러 전송 - DiaryId: {}, Error: {}", diaryId, errorContent);
                }
                default -> {
                    log.warn("⚠️ 알 수 없는 콜백 타입 - Type: {}, DiaryId: {}", callbackType, diaryId);
                }
            }
            
            // 콜백 데이터 저장 (선택적)
            callbackDataService.saveCallbackData(diaryId, callbackType, payload);
            acknowledgment.acknowledge();
            
        } catch (Exception e) {
            log.error("❌ AI 콜백 처리 실패 - Message: {}", message, e);
            acknowledgment.acknowledge(); // 실패한 메시지도 acknowledge (재처리 방지)
        }
    }
    
    /**
     * 메시지가 TTL을 초과했는지 확인
     */
    private boolean isMessageExpired(long messageTimestamp) {
        long currentTime = System.currentTimeMillis();
        long messageAge = currentTime - messageTimestamp;
        long ttlMillis = messageTtlMinutes * 60 * 1000L;
        return messageAge > ttlMillis;
    }
    
    /**
     * 메시지의 나이를 분 단위로 계산
     */
    private long getMessageAgeMinutes(long messageTimestamp) {
        long currentTime = System.currentTimeMillis();
        long messageAge = currentTime - messageTimestamp;
        return messageAge / (60 * 1000L);
    }
} 