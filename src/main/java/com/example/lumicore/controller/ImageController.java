package com.example.lumicore.controller;

import com.example.lumicore.dto.readSession.ReadSessionResponse;
import com.example.lumicore.dto.uploadSession.UploadParRequest;
import com.example.lumicore.dto.uploadSession.UploadSessionResponse;
import com.example.lumicore.service.ImageService;

import com.example.lumicore.service.QueueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/core/images")
@RequiredArgsConstructor
@Slf4j

@Tag(name = "Image", description = "이미지 처리 관련 API")
public class ImageController {

    private final ImageService imageService;
    private final QueueService queueService;

    /**
     * POST /api/images/start-session
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

    /**
     * GET /api/images/session/{diaryId}
     * → ReadSessionResponse DTO를 OCI Queue로 발행하고,
     *    클라이언트에는 HTTP 200 OK 만 돌려줍니다.
     */
    @Operation(
            summary = "READ PAR 생성 & Queue 발행",
            description = "읽기 Pre-Authenticated Request(PAR) 리스트를 생성한 뒤, OCI Queue로 발행합니다. 클라이언트에게는 " +
                    "HTTP 200 OK 만 반환합니다."
    )
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
