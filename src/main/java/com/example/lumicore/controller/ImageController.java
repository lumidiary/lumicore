package com.example.lumicore.controller;

import com.example.lumicore.dto.DiaryPhotoDto;
import com.example.lumicore.service.ImageService;
import com.example.lumicore.service.ImageServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 이미지 업로드 요청을 처리하는 REST 컨트롤러
 */
@RestController
@RequestMapping("/api/images")
@Slf4j
@RequiredArgsConstructor
public class ImageController {

    private final ImageService imageService;

    @PostMapping("/upload")
    public ResponseEntity<DiaryPhotoDto> uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            DiaryPhotoDto dto = imageService.uploadImage(file);
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }
}
