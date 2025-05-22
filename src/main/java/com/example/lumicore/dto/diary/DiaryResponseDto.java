package com.example.lumicore.dto.diary;


import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class DiaryResponseDto {

    private UUID diaryId;
    private UUID userId;
    private String userLocale;
    private String emotionTag;
    private String overallDaySummary;
    private LocalDateTime createdAt;
    private List<QuestionAnswerDiaryDto> answers;
    private List<PhotoInfoDto> photos;

}
