package com.example.lumicore.dto.Digest.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DigestResponseDto {

    private UUID userId;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private String title;
    private String overallEmotion;

    private String activity;
    private String emotionTrend;
    private String specialMoment;

    private List<DigestResponseEntryDto> entries;

}
