package com.example.lumicore.dto.Digest.request;

import com.example.lumicore.dto.Diary.QuestionAnswerDiaryDto;
import com.example.lumicore.jpa.entity.EmotionTag;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class DigestRequestEntryDto {

    private UUID diaryId;
    private LocalDateTime createdAt;
    private EmotionTag emotionTag;
    private String overallDaySummary;

    private List<QuestionAnswerDiaryDto> answers;
    private List<DigestPhotoInfoDto> photos;


}
