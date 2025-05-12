package com.example.lumicore.dto;


import com.example.lumicore.jpa.entity.EmotionTag;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 2) POST /diaries/{diaryId}/answers 또는
 *    GET  /diaries/{diaryId} 최종 조회용
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiaryDetailResponseDto {
    private Long id;
    private Long userId;
    private String content;
    private EmotionTag emotion;
    private List<DiaryPhotoDto> photos;
    private List<QaDto> qas;
    private LocalDateTime createdAt;
}