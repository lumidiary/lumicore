package com.example.lumicore.dto.analysis;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ImageAnalysisDto {
    @JsonProperty("id")
    private String imageId;
    private String description;
    private MetadataDto metadata;
}
