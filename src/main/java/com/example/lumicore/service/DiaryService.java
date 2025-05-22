package com.example.lumicore.service;

import com.example.lumicore.dto.diary.DiaryResponseDto;
import com.example.lumicore.dto.diary.DiarySummaryDto;
import com.example.lumicore.dto.digest.request.DigestRequestEntryDto;
import com.example.lumicore.dto.question.DiaryAnswerRequestDto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;


public interface DiaryService {

    void submitDiaryAnswers(DiaryAnswerRequestDto requestDto) throws Exception;

    void deleteDiary(UUID diaryId) throws Exception;

    DiaryResponseDto getDiary(UUID diaryId) throws Exception;

    List<DiarySummaryDto> getDiariesByUser(UUID userId) throws Exception;

    List<UUID> getAllUserIds();

    /** 사용자 로케일 조회 (예: "ko", "en") → DigestRequestDto.userLocale */
    String getUserLocale(UUID userId);

    /** 해당 기간의 DigestRequestEntryDto 목록 생성 → Batch Processor 用 */
    List<DigestRequestEntryDto> getEntriesForDigest(
            UUID userId,
            LocalDateTime periodStart,
            LocalDateTime periodEnd
    );


}
