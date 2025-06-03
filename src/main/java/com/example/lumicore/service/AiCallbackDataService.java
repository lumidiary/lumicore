package com.example.lumicore.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiCallbackDataService {

    // 메모리 기반 캐시 (실제 운영에서는 Redis나 DB 사용 권장)
    private final Map<String, List<Map<String, Object>>> callbackHistory = new ConcurrentHashMap<>();

    /**
     * 콜백 데이터를 저장합니다.
     */
    public void saveCallbackData(String diaryId, String callbackType, JsonNode data) {
        try {
            Map<String, Object> callbackData = new java.util.HashMap<>();
            callbackData.put("diaryId", diaryId);
            callbackData.put("callbackType", callbackType);
            callbackData.put("data", data);
            callbackData.put("timestamp", System.currentTimeMillis());
            
            callbackHistory.computeIfAbsent(diaryId, k -> new ArrayList<>()).add(callbackData);
            
            log.debug("💾 콜백 데이터 저장 완료 - DiaryId: {}, Type: {}", diaryId, callbackType);
            
            // 최대 10개까지만 유지 (메모리 절약)
            List<Map<String, Object>> history = callbackHistory.get(diaryId);
            if (history.size() > 10) {
                history.remove(0);
            }
            
        } catch (Exception e) {
            log.error("❌ 콜백 데이터 저장 실패: {}", e.getMessage(), e);
        }
    }

    /**
     * 특정 diaryId의 콜백 히스토리를 조회합니다.
     */
    public List<Map<String, Object>> getCallbackHistory(String diaryId) {
        return callbackHistory.getOrDefault(diaryId, new ArrayList<>());
    }

    /**
     * 콜백 히스토리를 정리합니다.
     */
    public void clearCallbackHistory(String diaryId) {
        callbackHistory.remove(diaryId);
        log.debug("🧹 콜백 히스토리 정리 완료 - DiaryId: {}", diaryId);
    }

    /**
     * 최신 콜백을 조회합니다.
     */
    public Map<String, Object> getLatestCallback(String diaryId) {
        List<Map<String, Object>> history = callbackHistory.get(diaryId);
        if (history != null && !history.isEmpty()) {
            return history.get(history.size() - 1);
        }
        return null;
    }

    /**
     * 특정 타입의 최신 콜백을 조회합니다.
     */
    public Map<String, Object> getLatestCallbackByType(String diaryId, String callbackType) {
        List<Map<String, Object>> history = callbackHistory.get(diaryId);
        if (history == null) {
            return null;
        }

        return history.stream()
                .filter(callback -> callbackType.equals(callback.get("callbackType")))
                .reduce((first, second) -> second)
                .orElse(null);
    }
} 