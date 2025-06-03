package com.example.lumicore.dto.uploadSession;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
import java.util.UUID;

/**
 * 생성된 Diary ID 와 PAR 리스트를 함께 반환
 */
@Data
@AllArgsConstructor
public class UploadSessionResponse {
    private UUID diaryId;
    private List<UploadParDto> uploadPars;
}
