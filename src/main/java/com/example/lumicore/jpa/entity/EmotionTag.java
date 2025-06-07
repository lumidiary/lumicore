package com.example.lumicore.jpa.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 일기 작성 시 선택 가능한 감정 태그
 */
@Getter
@RequiredArgsConstructor
public enum EmotionTag {
    HAPPY,
    JOY,
    NEUTRAL,
    ANGRY,
    SAD,
    GOOD;  // 기존 데이터 호환성을 위해 임시 유지

    @JsonCreator
    public static EmotionTag from(String value) {
        try {
            return EmotionTag.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid EmotionTag value: " + value + ", defaulting to NEUTRAL");
            return EmotionTag.NEUTRAL;
        }
    }
}
