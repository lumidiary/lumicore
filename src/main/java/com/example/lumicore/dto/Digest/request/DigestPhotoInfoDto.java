package com.example.lumicore.dto.Digest.request;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class DigestPhotoInfoDto {

    private UUID photoId;
    private String description;
    private LocalDateTime capturedAt;
    private Double latitude;
    private Double longitude;
}
