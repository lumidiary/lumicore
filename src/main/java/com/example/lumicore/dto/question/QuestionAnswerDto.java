package com.example.lumicore.dto.question;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class QuestionAnswerDto {
    private UUID id;
    private String answer;

}
