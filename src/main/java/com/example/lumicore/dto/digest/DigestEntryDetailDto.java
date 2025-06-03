package com.example.lumicore.dto.digest;

import com.example.lumicore.jpa.entity.EmotionTag;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class DigestEntryDetailDto {
    private UUID diaryId;
    private LocalDateTime capturedAt;
    private EmotionTag emotion;
    private Double latitude;
    private Double longitude;
    private String summary;
    private String imageUrl;
}

