package com.example.lumicore.jpa.repository;

import com.example.lumicore.jpa.entity.Diary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DiaryRepository extends JpaRepository<Diary, Long> {
    Optional<Diary> findByIdAndUserIdAndDeletedAtIsNull(Long id, Long userId);
    List<Diary> findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long userId);
}

