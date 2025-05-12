package com.example.lumicore.vo;


import com.example.lumicore.jpa.entity.EmotionTag;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 서비스 내부 로직용 VO
 * - userId는 JWT에서 추출해 세팅
 * - soft delete 관련 필드는 제외
 */
@Value
public class DiaryVo {
    Long diaryId;
    Long userId;               // JWT에서 확보
    EmotionTag emotion;
    List<String> imageUrls;
    List<QaVo> qas;            // AI 문답 리스트
    LocalDateTime createdAt;
}
