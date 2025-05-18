package com.example.lumicore.jpa.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "landmark")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Landmark {

    @Id
    @Column(name = "id", length = 100, updatable = false, nullable = false)
    private String id;  // Google Place ID

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    /** 중간 엔티티 1:N */
    @OneToMany(mappedBy = "landmark", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<PhotoLandmark> photoLandmarks = new HashSet<>();
}
