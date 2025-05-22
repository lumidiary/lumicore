package com.example.lumicore.jpa.repository;

import com.example.lumicore.jpa.entity.DigestEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DigestEntryRepository extends JpaRepository<DigestEntry, UUID>{
    boolean existsByDigestIdAndDiaryId(UUID digestId, UUID diaryId);

}
