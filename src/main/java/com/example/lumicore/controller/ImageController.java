package com.example.lumicore.controller;

import com.example.lumicore.dto.uploadSession.UploadParRequest;
import com.example.lumicore.dto.uploadSession.UploadSessionResponse;
import com.example.lumicore.service.ImageService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
@Slf4j
public class ImageController {

    private final ImageService imageService;

    /**
     * POST /api/images/start-session
     * body: { "fileNames": ["a.png","b.jpg", ...] }
     * → diaryId와 PAR 리스트 반환
     */
    @PostMapping("/start-session")
    public ResponseEntity<UploadSessionResponse> startSession(
            @RequestBody UploadParRequest request) {
        try {
            UploadSessionResponse resp = imageService.startUploadSession(request);
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            log.error("업로드 세션 생성 실패", e);
            return ResponseEntity.status(500).build();
        }
    }
}
