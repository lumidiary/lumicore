package com.example.lumicore.jpa.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 일기 작성 시 선택 가능한 감정 태그 (5단계: 매우 좋음→매우 나쁨)
 */
@Getter
@RequiredArgsConstructor
public enum EmotionTag {
    VERY_GOOD("아주 좋은"),
    GOOD     ("좋은"),
    NEUTRAL  ("보통"),
    BAD      ("나쁨"),
    VERY_BAD ("매우 나쁨");

    private final String label;  // 화면에 보여줄 한글

    @Override
    public String toString() {
        return label;
    }
}
