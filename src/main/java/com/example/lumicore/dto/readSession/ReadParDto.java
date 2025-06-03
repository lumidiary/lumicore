package com.example.lumicore.dto.readSession;


import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

/**
 * 단일 이미지의 읽기용 PAR 정보
 */
@Data
@AllArgsConstructor
public class ReadParDto {
    private UUID id;
    private String accessUri;
}
