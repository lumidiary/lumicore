package com.example.lumicore.dto.question;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class QuestionItemDto {
    private UUID id;
    private String question;
}
