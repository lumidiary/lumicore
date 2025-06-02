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
    SAD;

    @JsonCreator
    public static EmotionTag from(String value) {
        return EmotionTag.valueOf(value.trim().toUpperCase());
    }

}
