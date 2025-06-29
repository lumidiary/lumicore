package com.example.lumicore.controller;

import com.example.lumicore.dto.uploadSession.UploadParRequest;
import com.example.lumicore.dto.uploadSession.UploadSessionResponse;
import com.example.lumicore.service.ImageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/core/images")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Image", description = "이미지 처리 관련 API")
public class ImageController {

    private final ImageService imageService;

    /**
     * POST /core/images/session
     * body: { "fileNames": ["a.png","b.jpg", ...] }
     * → diaryId와 PAR 리스트 반환
     */
    @Operation(
            summary = "Write PAR 생성",
            description = "클라이언트에게 파일명 List 요청이 들어오면, diaryId와 Pre-Authenticated Request(PAR) 리스트를 반환합니다."
    )
    @PostMapping("/session")
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