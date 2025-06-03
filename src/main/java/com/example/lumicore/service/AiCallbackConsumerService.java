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

import static com.example.lumicore.service.AiCallbackProducerService.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiCallbackConsumerService {

    private final DiaryWebSocketHandler webSocketHandler;
    private final ObjectMapper objectMapper;
    private final AiCallbackDataService callbackDataService;

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
            System.out.println("[Kafka DEBUG] AiCallbackConsumerService 핸들러 진입: " + message);
            log.info("🎯 AI 콜백 수신 - Topic: {}, Partition: {}, Offset: {}", topic, partition, offset);
            log.debug("📥 콜백 메시지: {}", message);
            
            // JSON 파싱
            JsonNode callbackJson = objectMapper.readTree(message);
            String diaryId = callbackJson.get("diaryId").asText();
            String callbackType = callbackJson.get("callbackType").asText();
            JsonNode data = callbackJson.get("data");
            
            log.info("🔍 콜백 처리 시작 - DiaryId: {}, Type: {}", diaryId, callbackType);
            
            // 현재 Pod에 해당 diaryId의 WebSocket 세션이 있는지 확인
            if (!webSocketHandler.hasLocalSession(diaryId)) {
                log.info("👻 현재 Pod에 해당 세션 없음, 무시 - DiaryId: {}", diaryId);
                acknowledgment.acknowledge();
                return;
            }
            
            // 콜백 데이터 저장
            callbackDataService.saveCallbackData(diaryId, callbackType, data);
            
            // 콜백 타입에 따라 처리
            processCallback(diaryId, callbackType, data);
            
            log.info("✅ 콜백 처리 완료 - DiaryId: {}", diaryId);
            acknowledgment.acknowledge();
            
        } catch (Exception e) {
            log.error("❌ AI 콜백 처리 실패: {}", e.getMessage(), e);
            // 에러 발생시에도 acknowledge 하여 무한 재시도를 방지
            acknowledgment.acknowledge();
        }
    }

    /**
     * 콜백 타입에 따라 적절한 처리를 수행합니다.
     */
    private void processCallback(String diaryId, String callbackType, JsonNode data) {
        switch (callbackType) {
            case AiCallbackProducerService.CALLBACK_TYPE_SESSION_PREPARE:
                handleSessionPrepareBroadcast(diaryId);
                break;
            case CALLBACK_TYPE_QUESTION:
                handleQuestionCallback(diaryId, data);
                break;
                
            case CALLBACK_TYPE_ANALYSIS_COMPLETE:
                handleAnalysisCompleteCallback(diaryId, data);
                break;
                
            case CALLBACK_TYPE_DIGEST_COMPLETE:
                handleDigestCompleteCallback(diaryId, data);
                break;
                
            case CALLBACK_TYPE_ERROR:
                handleErrorCallback(diaryId, data);
                break;
                
            default:
                log.warn("⚠️ 알 수 없는 콜백 타입: {}", callbackType);
        }
    }

    private void handleSessionPrepareBroadcast(String diaryId) {
        log.info("🎯 세션 준비 브로드캐스트 수신 - DiaryId: {}", diaryId);
        webSocketHandler.prepareSession(diaryId);
    }

    /**
     * 질문 생성 완료 콜백 처리 - 세션 정리 추가
     */
    private void handleQuestionCallback(String diaryId, JsonNode data) {
        log.info("📝 질문 생성 완료 콜백 처리 - DiaryId: {}", diaryId);
        
        try {
            // QuestionListResponseDto인 경우
            if (data.has("questions")) {
                QuestionListResponseDto questions = objectMapper.treeToValue(data, QuestionListResponseDto.class);
                String questionText = questions.getQuestions().stream()
                        .map(q -> q.getQuestion())
                        .collect(java.util.stream.Collectors.joining("\n"));
                webSocketHandler.sendQuestionsComplete(diaryId, questionText); // 정리 포함된 메서드 사용
            }
            // 단순 문자열 content인 경우
            else if (data.has("content")) {
                String content = data.get("content").asText();
                String status = data.has("status") ? data.get("status").asText() : "SUCCESS";
                
                if ("SUCCESS".equals(status)) {
                    // 기존 sendQuestions 대신 연결 종료 포함된 메서드 사용
                    webSocketHandler.sendQuestionsAndRequestDisconnect(diaryId, content);
                } else {
                    webSocketHandler.sendError(diaryId, "질문 생성에 실패했습니다.");
                }
            }
        } catch (Exception e) {
            log.error("❌ 질문 콜백 처리 실패 - DiaryId: {}", diaryId, e);
            webSocketHandler.sendError(diaryId, "질문 생성에 실패했습니다.");
        }
    }

    /**
     * 분석 완료 콜백 처리
     */
    private void handleAnalysisCompleteCallback(String diaryId, JsonNode data) {
        log.info("📊 분석 완료 콜백 처리 - DiaryId: {}", diaryId);
        
        String status = data.has("status") ? data.get("status").asText() : "SUCCESS";
        if ("SUCCESS".equals(status)) {
            webSocketHandler.sendAnalysisComplete(diaryId);
        } else {
            log.error("❌ 분석 실패 - DiaryId: {}", diaryId);
            webSocketHandler.sendError(diaryId, "분석에 실패했습니다.");
        }
    }

    /**
     * 다이제스트 완료 콜백 처리
     */
    private void handleDigestCompleteCallback(String diaryId, JsonNode data) {
        log.info("📚 다이제스트 완료 콜백 처리 - DiaryId: {}", diaryId);
        
        String status = data.has("status") ? data.get("status").asText() : "SUCCESS";
        if ("SUCCESS".equals(status)) {
            String digestContent = data.has("digestContent") ? data.get("digestContent").asText() : "";
            webSocketHandler.sendDigestComplete(diaryId, digestContent);
        } else {
            log.error("❌ 다이제스트 실패 - DiaryId: {}", diaryId);
            webSocketHandler.sendError(diaryId, "다이제스트 생성에 실패했습니다.");
        }
    }

    /**
     * 에러 콜백 처리
     */
    private void handleErrorCallback(String diaryId, JsonNode data) {
        String errorMessage = data.has("errorMessage") ? data.get("errorMessage").asText() : "AI 서비스에서 오류가 발생했습니다.";
        log.error("💥 AI 서비스 에러 콜백 - DiaryId: {}, Error: {}", diaryId, errorMessage);
        webSocketHandler.sendError(diaryId, errorMessage);
    }
} 