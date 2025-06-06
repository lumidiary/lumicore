package com.example.lumicore.jpa.repository;

import com.example.lumicore.jpa.entity.DiaryPhoto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * DiaryPhoto 엔티티를 저장·조회하기 위한 JPA 리포지토리
 */
@Repository
public interface DiaryPhotoRepository extends JpaRepository<DiaryPhoto, UUID> {

    List<DiaryPhoto> findByDiaryId(UUID diaryId);
}
