package com.example.lumicore.jpa.repository;

import com.example.lumicore.jpa.entity.Landmark;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LandmarkRepository extends JpaRepository<Landmark, String> { }
