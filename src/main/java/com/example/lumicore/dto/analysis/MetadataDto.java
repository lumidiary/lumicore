package com.example.lumicore.dto.analysis;

import lombok.Data;

import java.util.List;

@Data
public class MetadataDto {
    private String captureDate;
    private LocationDto location;
    private List<LandmarkDto> nearbyLandmarks;
}

