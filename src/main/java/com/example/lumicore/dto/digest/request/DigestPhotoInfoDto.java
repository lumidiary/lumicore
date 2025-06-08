package com.example.lumicore.dto.digest.request;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DigestPhotoInfoDto {

    private Integer index;
    private String description;
    private Double latitude;
    private Double longitude;
}
