package com.example.lumicore.dto.analysis;

import lombok.Data;

import java.util.List;

@Data
public class AnalysisResultDto {
    private List<ImageAnalysisDto> images;
    private String overallDaySummary;
    private List<String> questions;
    private String language;
}
