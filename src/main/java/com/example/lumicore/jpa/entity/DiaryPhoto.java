package com.example.lumicore.jpa.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "diary_photos")
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DiaryPhoto extends BaseEntity {

    @Id
    @UuidGenerator
    @GeneratedValue
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "diary_id", nullable = false)
    private Diary diary;

    @Column(name = "object_key", length = 1000, nullable = false)
    private String objectKey;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "captured_at")
    private LocalDateTime capturedAt;

    /** 위도 */
    @Column(name = "latitude")
    private Double latitude;

    /** 경도 */
    @Column(name = "longitude")
    private Double longitude;

    /** 중간 엔티티 1:N */
    @OneToMany(mappedBy = "photo", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<PhotoLandmark> photoLandmarks = new HashSet<>();

    /** helper: static of()을 이용해 중간 엔티티 생성 */
    public void addLandmark(Landmark lm) {
        PhotoLandmark pl = PhotoLandmark.of(this, lm);
        this.photoLandmarks.add(pl);
    }

    public void updateDescription(String description) {
        this.description = description;
    }

    public void updateCapturedAt(LocalDateTime capturedAt) {
        this.capturedAt = capturedAt;
    }

    public void updateLocation(Double latitude, Double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public static DiaryPhoto of(Diary diary, String objectKey) {
        return DiaryPhoto.builder()
                .diary(diary)
                .objectKey(objectKey)
                .build();
    }
}
