package com.example.lumicore.jpa.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Entity
@Table(name = "digest_entries",
        uniqueConstraints = {@UniqueConstraint(columnNames = {"digest_id", "diary_id"})})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder

public class DigestEntry {

    @Id
    @UuidGenerator
    @GeneratedValue
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "digest_id", nullable = false)
    private Digest digest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "diary_id", nullable = false)
    private Diary diary;

    @Column(name = "diary_summary", length = 2000)
    private String diarySummary;

    /** bi-directional 설정을 위한 helper */
    public void updateDigest(Digest digest) {
        this.digest = digest;
    }
}
