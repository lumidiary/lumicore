package com.example.lumicore.dto.analysis;

import lombok.Data;

import java.util.List;

@Data
public class MetadataDto {
    private String captureDate;
    private Double latitude;
    private Double longitude;
    private List<LandmarkDto> nearbyLandmarks;
}
