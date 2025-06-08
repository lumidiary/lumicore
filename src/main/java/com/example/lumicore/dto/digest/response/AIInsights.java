package com.example.lumicore.dto.digest.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AIInsights {
    private String activity;
    private String emotionTrend;
    private String specialMoment;
}
