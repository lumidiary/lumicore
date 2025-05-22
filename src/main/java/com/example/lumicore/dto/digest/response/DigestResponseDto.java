package com.example.lumicore.dto.digest.response;

import com.fasterxml.jackson.annotation.JsonProperty;
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

    private UUID id;
    private String title;
    private String overallEmotion;
    private PeriodDto period;

    private AiInsightsDto aiInsights;

    @JsonProperty("summary")
    private String digestSummary;

    private List<DigestResponseEntryDto> entries;

}
