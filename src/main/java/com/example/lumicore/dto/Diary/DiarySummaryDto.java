package com.example.lumicore.dto.Diary;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class DiarySummaryDto {
    private UUID diaryId;
    private LocalDateTime createdAt;
    private QuestionAnswerDiaryDto firstAnswer;
    private PhotoInfoDto firstPhoto;
}
