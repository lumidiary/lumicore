package com.example.lumicore.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 5) 사진 정보 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiaryPhotoDto {
    private String objectKey;   // 저장된 Object key
    private String accessUri;   // READ-PAR URL (AI 호출용)
}