package com.example.lumicore.dto.digest.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DigestResponseDto {

    private String id;
    private Period period;
    private String title;

    private String overallEmotion;
    @JsonProperty("summary")
    private String digestSummary;

    private AIInsights aiInsights;

    private List<EntryDigest> entries;

}
