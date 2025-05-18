package com.example.lumicore.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
public class QuestionListResponseDto {
    private UUID diaryId;
    private List<String> questions;
}
