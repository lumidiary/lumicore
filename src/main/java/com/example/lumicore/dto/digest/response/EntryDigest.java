package com.example.lumicore.dto.digest.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EntryDigest {
    @JsonProperty("id")
    private String diaryId;

    @JsonProperty("summary")
    private String diarySummary;
}
