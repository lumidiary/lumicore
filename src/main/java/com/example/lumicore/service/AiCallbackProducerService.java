package com.example.lumicore.service;

import com.example.lumicore.dto.analysis.AnalysisCompleteResponseDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiCallbackProducerService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.kafka.topic.ai-callback}")
    private String aiCallbackTopic;

    // 콜백 타입 상수들 (QUESTION 제거)
    public static final String CALLBACK_TYPE_ANALYSIS_COMPLETE = "ANALYSIS_COMPLETE";
    public static final String CALLBACK_TYPE_DIGEST_COMPLETE = "DIGEST_COMPLETE";
    public static final String CALLBACK_TYPE_ERROR = "ERROR";

    /**
     * Kafka를 통해 콜백 메시지 전송
     */
    public void sendCallback(String diaryId, String callbackType, Object data) {
        try {
            Map<String, Object> callbackMessage = new HashMap<>();
            callbackMessage.put("diaryId", diaryId);
            callbackMessage.put("callbackType", callbackType);
            callbackMessage.put("timestamp", System.currentTimeMillis());
            callbackMessage.put("data", data);

            String jsonMessage = objectMapper.writeValueAsString(callbackMessage);
            kafkaTemplate.send(aiCallbackTopic, diaryId, jsonMessage);
            
            log.info("📤 Kafka 콜백 전송 성공: diaryId={} , type={}", diaryId, callbackType);
        } catch (Exception e) {
            log.error("❌ Kafka 콜백 전송 실패: diaryId={}, type={}", diaryId, callbackType, e);
            throw new RuntimeException("Failed to send callback", e);
        }
    }

    /**
     * 분석 완료 콜백 전송 (JSON 데이터 포함)
     */
    public void sendAnalysisCompleteCallback(String diaryId, AnalysisCompleteResponseDto analysisData) {
        sendCallback(diaryId, CALLBACK_TYPE_ANALYSIS_COMPLETE, analysisData);
    }

    /**
     * 다이제스트 완료 콜백 전송
     */
    public void sendDigestCompleteCallback(String diaryId, String digestContent) {
        Map<String, Object> data = new HashMap<>();
        data.put("digestContent", digestContent);
        data.put("status", "SUCCESS");
        sendCallback(diaryId, CALLBACK_TYPE_DIGEST_COMPLETE, data);
    }

    /**
     * 에러 콜백 전송
     */
    public void sendErrorCallback(String diaryId, String errorMessage, String serviceType) {
        Map<String, Object> data = new HashMap<>();
        data.put("status", "ERROR");
        data.put("errorMessage", errorMessage);
        data.put("serviceType", serviceType);
        data.put("timestamp", System.currentTimeMillis());
        sendCallback(diaryId, CALLBACK_TYPE_ERROR, data);
    }
} 