package com.example.lumicore.jpa.entity;

import jakarta.persistence.*;
import lombok.*;


@Entity
@Table(name = "diary_photos")
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DiaryPhoto extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "diary_id", nullable = false)
    private Diary diary;

    @Column(length = 1000, nullable = false)
    private String imageUrl;

    /** 이미지 URL 갱신 */
    public void updateImageUrl(String url) {
        this.imageUrl = url;
    }
}
