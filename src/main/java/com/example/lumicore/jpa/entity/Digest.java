package com.example.lumicore.jpa.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

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
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(
            name = "UUID",
            strategy = "org.hibernate.id.UUIDGenerator"
    )
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    private UUID userId;

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

    @Column(name = "digest_summary", length = 1000)
    private String digestSummary;

    @OneToMany(mappedBy = "digest", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DigestEntry> entries = new ArrayList<>();
}

