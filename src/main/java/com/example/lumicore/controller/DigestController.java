package com.example.lumicore.controller;

import com.example.lumicore.dto.digest.DigestDetailDto;
import com.example.lumicore.dto.digest.DigestSummaryDto;
import com.example.lumicore.dto.digest.response.DigestResponseDto;
import com.example.lumicore.jpa.entity.Digest;
import com.example.lumicore.service.DigestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/core/digests")
@RequiredArgsConstructor
public class DigestController {

    private final DigestService digestService;
    private static final Logger log = LoggerFactory.getLogger(DigestController.class);

    /**
     * 1) JSON 바디로 DigestResponseDto 받기
     * 2) 저장 → 조회 → HTML 페이로드 생성
     * 3) 하드코딩된 URL로 POST 푸시
     */
    @PostMapping
    public ResponseEntity<Void> createAndNotify(
            @RequestBody DigestResponseDto requestDto
    ) {
        // 실제 받은 JSON 전체를 로그로 출력
        log.info("Raw DigestResponseDto: {}", requestDto);
        
        // 1) Digest 저장
        UUID digestId = digestService.saveDigestFromResponse(requestDto);

        // 2) 저장된 Digest 조회
        Digest digest = digestService.findDigestById(digestId);

        // 3) DigestSummaryDto로 페이로드 생성 (HTML 제거)
        DigestSummaryDto payload = new DigestSummaryDto();
        payload.setId(digest.getUserId());
        payload.setTitle(digest.getTitle());
        payload.setPeriodStart(digest.getPeriodStart());
        payload.setPeriodEnd(digest.getPeriodEnd());
        payload.setSummary(digest.getDigestSummary());

        // 4) RestTemplate 바로 생성 & 하드코딩된 URL로 POST
        String callbackUrl = "http://api.lumidiary.com/users/digest/completed";
        new RestTemplate()
                .postForEntity(callbackUrl, payload, Void.class);

        // 5) 호출한 클라이언트에는 204 No Content만 반환
        return ResponseEntity.noContent().build();
    }


    @GetMapping("/user/{userId}")
    public ResponseEntity<List<DigestSummaryDto>> getByUser(
            @PathVariable UUID userId
    ) {
        List<DigestSummaryDto> list = digestService.getDigestsByUser(userId);
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{digestId}")
    public ResponseEntity<DigestDetailDto> getDetails(
            @PathVariable UUID digestId
    ) {
        DigestDetailDto dto = digestService.getDigestDetails(digestId);
        return ResponseEntity.ok(dto);
    }


}

