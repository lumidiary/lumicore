package com.example.lumicore.dto.analysis;

import lombok.Data;

@Data
public class ImageAnalysisDto {
    private String imageId;
    private String description;
    private MetadataDto metadata;
}
