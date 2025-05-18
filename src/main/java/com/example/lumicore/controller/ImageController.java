package com.example.lumicore.controller;

import com.example.lumicore.dto.readSession.ReadSessionResponse;
import com.example.lumicore.dto.uploadSession.UploadParRequest;
import com.example.lumicore.dto.uploadSession.UploadSessionResponse;
import com.example.lumicore.service.ImageService;

import com.example.lumicore.service.QueueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
@Slf4j
public class ImageController {

    private final ImageService imageService;
    private final QueueService queueService;

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

    /**
     * GET /api/images/session/{diaryId}
     * → ReadSessionResponse DTO를 OCI Queue로 발행하고,
     *    클라이언트에는 HTTP 200 OK 만 돌려줍니다.
     */
    @GetMapping("/session/{diaryId}")
    public ResponseEntity<Void> enqueueReadSession(@PathVariable UUID diaryId) {
        try {
            ReadSessionResponse dto = imageService.generateReadSession(diaryId);
            queueService.sendReadSession(dto);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            log.warn("Invalid diaryId for read session: {}", diaryId, e);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Failed to enqueue read session for diaryId={}", diaryId, e);
            return ResponseEntity.status(500).build();
        }
    }

}
