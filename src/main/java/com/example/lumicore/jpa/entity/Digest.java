package com.example.lumicore.jpa.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "digests")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Digest {

    @Id
    @Column(columnDefinition = "UUID")
    private UUID id;

    private UUID userId;

    @Enumerated(EnumType.STRING)
    private PeriodType periodType;

    private LocalDate periodStart;
    private LocalDate periodEnd;

    private String title;
    private String overallEmotion;

    @Column(name = "activity", length = 1000)
    private String activity;

    @Column(name = "emotion_trend", length = 1000)
    private String emotionTrend;

    @Column(name = "special_moment", length = 1000)
    private String specialMoment;

    @OneToMany(mappedBy = "digest", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DigestEntry> entries = new ArrayList<>();
}

