package com.example.lumicore.dto.digest.request;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DigestQuestionDto {
    private Integer index;
    private String question;
    private String answer;
} 