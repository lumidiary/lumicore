package com.example.lumicore.dto.digest.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DigestResponseEntryDto {
    @JsonProperty("id")
    private UUID diaryId;

    @JsonProperty("summary")
    private String diarySummary;
}
