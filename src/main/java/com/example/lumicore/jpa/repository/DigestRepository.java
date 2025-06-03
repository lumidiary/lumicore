package com.example.lumicore.jpa.repository;

import com.example.lumicore.jpa.entity.Digest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DigestRepository extends JpaRepository<Digest, UUID> {
    List<Digest> findAllByUserId(UUID userId);
}
