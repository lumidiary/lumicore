package com.example.lumicore.dto.Diary;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class PhotoInfoDto {

    private UUID photoId;
    private String url;       // READ-PAR URL
    private Double latitude;
    private Double longitude;

}
