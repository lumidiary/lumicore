package com.example.lumicore.jpa.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "diaries")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Diary extends BaseEntity {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(
            name = "UUID",
            strategy = "org.hibernate.id.UUIDGenerator"
    )
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "emotion", nullable = true)
    private EmotionTag emotion;

    @OneToMany(mappedBy = "diary", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DiaryPhoto> photos;

    @OneToMany(mappedBy = "diary", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DiaryQA> qas;

    @Column(name = "overall_day_summary", length = 1000)
    private String overallDaySummary;

    /** 사용자 로케일 (예: "ko", "en") */
    @Builder.Default
    @Column(name = "user_locale",
            nullable = false,
            length = 5,
            columnDefinition = "VARCHAR(5) DEFAULT 'ko'")
    private String userLocale = "ko";

    @OneToMany(mappedBy = "diary", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DigestEntry> digestEntries;


    /** 감정 변경 */
    public void changeEmotion(EmotionTag newEmotion) {
        this.emotion = newEmotion;
    }

    public void updateUserId(UUID userId) {
        this.userId = userId;
    }

    public void updateEmotionTag(EmotionTag emotionTag) {
        this.emotion = emotionTag;
    }

    public void updateOverallDaySummary(String overallDaySummary) {
        this.overallDaySummary = overallDaySummary;
    }
}