package com.example.lumicore.dto.readSession;


import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 단일 이미지의 읽기용 PAR 정보
 */
@Data
@AllArgsConstructor
public class ImageData {
    private String id;
    private String url;
}
