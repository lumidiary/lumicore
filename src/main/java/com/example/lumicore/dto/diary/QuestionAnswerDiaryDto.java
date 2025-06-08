package com.example.lumicore.dto.diary;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class QuestionAnswerDiaryDto {
    private UUID questionId;
    private String question;
    private String answer;
}
