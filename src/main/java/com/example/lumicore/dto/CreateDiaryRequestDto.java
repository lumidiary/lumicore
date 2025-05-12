package com.example.lumicore.dto;

import com.example.lumicore.jpa.entity.EmotionTag;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 1) 일기 생성 요청용 DTO (본문 + 감정 + 사진 Multipart)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateDiaryRequestDto {
    private EmotionTag emotion;                      // 감정 태그
    private List<MultipartFile> photos;              // 1~4장
}
