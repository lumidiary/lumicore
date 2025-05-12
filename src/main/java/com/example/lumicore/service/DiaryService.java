package com.example.lumicore.service;

import com.example.lumicore.dto.*;

import java.time.LocalDate;
import java.util.List;

public interface DiaryService {
    /** 1) 일기 생성 */
    CreateDiaryResponseDto createDiary(Long userId, CreateDiaryRequestDto dto);

    /** 2) 단일 조회 */
    DiaryDetailResponseDto getDiary(Long userId, Long diaryId);

    /** 3) 다중 요약 조회 */
    List<DiarySummaryDto> getDiarySummaries(Long userId);

    /** 4) soft delete */
    void deleteDiary(Long userId, Long diaryId);


    /** 5) 다중 조회 (기간) */
    List<DiarySummaryDto> searchDiaries(Long userId, LocalDate start, LocalDate end);



}
