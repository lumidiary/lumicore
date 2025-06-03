package com.example.lumicore.dto.question;

import com.example.lumicore.jpa.entity.EmotionTag;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class DiaryAnswerRequestDto {
    private UUID diaryId;
    private UUID userId;
    private EmotionTag emotionTag;       // ← 변경: String → EmotionTag
    private List<QuestionAnswerDto> answers;
}
