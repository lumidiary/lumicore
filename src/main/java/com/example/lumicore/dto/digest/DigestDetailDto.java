package com.example.lumicore.dto.digest;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
public class DigestDetailDto {
    private UUID id;
    private UUID userId;
    private String title;
    private String overallEmotion;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private String activity;
    private String emotionTrend;
    private String specialMoment;
    private String summary;  // digestSummary
    private List<DigestEntryDetailDto> entries;
}
