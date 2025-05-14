package com.example.lumicore.jpa.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 일기 작성 시 선택 가능한 감정 태그 (5단계: 매우 좋음→매우 나쁨)
 */
@Getter
@RequiredArgsConstructor
public enum EmotionTag {
    VERY_GOOD,
    GOOD,
    NEUTRAL,
    BAD,
    VERY_BAD;


}
