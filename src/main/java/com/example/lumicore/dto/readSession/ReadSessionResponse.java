package com.example.lumicore.dto.readSession;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
import java.util.UUID;

/**
 * 클라이언트에 반환할 응답:
 * - diaryId
 * - userLocale
 * - 각 이미지별 READ-PAR 리스트
 */

@Data
@AllArgsConstructor
public class ReadSessionResponse {
    private UUID diaryId;
    private String userLocale;
    private List<ReadParDto> imgPars;
}