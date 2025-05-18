package com.example.lumicore.jpa.repository;

import com.example.lumicore.jpa.entity.DiaryQA;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DiaryQARepository extends JpaRepository<DiaryQA, UUID> { }
