package com.example.lumicore.vo;

import lombok.Value;

import java.time.LocalDateTime;

/** 서비스 내부 QA VO */

@Value
public class QaVo {
    Long qaId;
    String aiQuestion;
    String userAnswer;
    LocalDateTime answeredAt;
}
