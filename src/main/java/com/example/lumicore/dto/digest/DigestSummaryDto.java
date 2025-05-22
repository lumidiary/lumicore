package com.example.lumicore.dto.digest;

import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
public class DigestSummaryDto {
    private UUID id;
    private String title;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private String summary;
}

