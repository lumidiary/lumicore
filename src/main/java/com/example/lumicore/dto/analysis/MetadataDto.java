package com.example.lumicore.dto.analysis;

import lombok.Data;
import java.util.List;

@Data
public class MetadataDto {
    private String captureDate;            // "yyyy-MM-dd HH:mm:ss"
    private List<LandmarkDto> nearbyLandmarks;
}
