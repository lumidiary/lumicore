package com.example.lumicore.jpa.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

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
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
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
    @Column(name = "latitude", precision = 9, scale = 6)
    private Double latitude;

    /** 경도 */
    @Column(name = "longitude", precision = 9, scale = 6)
    private Double longitude;

    /** 중간 엔티티 1:N */
    @OneToMany(mappedBy = "photo", cascade = CascadeType.ALL, orphanRemoval = true)
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



    /** 파일명(경로 포함 없이)만 필요할 때 */
    public String getFileName() {
        int idx = objectKey.lastIndexOf('/');
        return idx >= 0 ? objectKey.substring(idx + 1) : objectKey;
    }

    /** 이미지 URL 전체를 저장해야 할 때 사용하는 메서드 (필요 없으면 제거) */
    public void updateImageUrl(String url) {
        this.objectKey = url;
    }


    public static DiaryPhoto of(Diary diary, String objectKey) {
        return DiaryPhoto.builder()
                .diary(diary)
                .objectKey(objectKey)
                .build();
    }
}
