package com.example.lumicore.jpa.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;
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
    @GenericGenerator(
            name = "UUID",
            strategy = "org.hibernate.id.UUIDGenerator"
    )
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "diary_id", nullable = false)
    private Diary diary;

    @Column(name = "object_key", length = 1000, nullable = false)
    private String objectKey;

    @Column(name = "description", length = 1000, nullable = true)
    private String description;

    @Column(name = "captured_at", nullable = true)
    private LocalDateTime capturedAt;


    /** 이미지 URL 갱신 */
    public void updateImageUrl(String url) {
        this.objectKey = url;
    }

    public String getObjectKey() {
        return objectKey.substring(objectKey.lastIndexOf("/") + 1);
    }

    public static DiaryPhoto of(Diary diary, String objectKey) {
        return DiaryPhoto.builder()
                .diary(diary)
                .objectKey(objectKey)
                .build();
    }

}
