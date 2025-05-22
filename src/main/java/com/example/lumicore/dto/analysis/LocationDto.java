package com.example.lumicore.dto.analysis;

import lombok.Data;

@Data
public class LocationDto {
    private Double latitude;
    private Double longitude;
    private String address; // 선택
}
