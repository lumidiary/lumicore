package com.example.lumicore.dto.uploadSession;


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
public class UploadParDto {
    private String objectKey;   // 저장된 Object key
    private String accessUri;   // Upload-PAR URL
}