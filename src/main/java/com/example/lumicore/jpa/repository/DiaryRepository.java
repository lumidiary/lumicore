package com.example.lumicore.jpa.repository;

import com.example.lumicore.jpa.entity.Diary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DiaryRepository extends JpaRepository<Diary, UUID> {

    List<Diary> findByUserId(UUID userId);

    List<Diary> findByUserIdAndCreatedAtBetween(
            UUID userId,
            LocalDateTime start,
            LocalDateTime end
    );
}

