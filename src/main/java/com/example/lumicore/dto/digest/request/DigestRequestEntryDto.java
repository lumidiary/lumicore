package com.example.lumicore.dto.digest.request;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class DigestRequestEntryDto {

    private String id;
    private LocalDateTime date;
    private String emotion;
    private List<DigestPhotoInfoDto> imageDescriptions;
    private String overallDaySummary;
    private List<DigestQuestionDto> questions;

}
