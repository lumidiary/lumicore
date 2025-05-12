package com.example.lumicore.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 6) 다중 조회용 요약 DTO (카드 뉴스 형태)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiarySummaryDto {
    private Long id;
    private LocalDateTime createdAt;
    private String imgUrl;
}

