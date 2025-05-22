package com.example.lumicore.service;

import com.example.lumicore.dto.digest.DigestDetailDto;
import com.example.lumicore.dto.digest.DigestEntryDetailDto;
import com.example.lumicore.dto.digest.DigestSummaryDto;
import com.example.lumicore.dto.digest.response.DigestResponseDto;
import com.example.lumicore.dto.digest.response.DigestResponseEntryDto;
import com.example.lumicore.jpa.entity.Diary;
import com.example.lumicore.jpa.entity.DiaryPhoto;
import com.example.lumicore.jpa.entity.Digest;
import com.example.lumicore.jpa.entity.DigestEntry;
import com.example.lumicore.jpa.repository.DiaryRepository;
import com.example.lumicore.jpa.repository.DigestEntryRepository;
import com.example.lumicore.jpa.repository.DigestRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DigestService {

    private final DigestRepository digestRepository;
    private final DiaryRepository diaryRepository;
    private final DigestEntryRepository digestEntryRepository;

    @Transactional
    public UUID saveDigestFromResponse(DigestResponseDto dto) {
        // 1) Digest 생성 및 저장
        Digest digest = Digest.builder()
                .userId(dto.getId())
                .periodStart(dto.getPeriod().getStart())
                .periodEnd(dto.getPeriod().getEnd())
                .title(dto.getTitle())
                .overallEmotion(dto.getOverallEmotion())
                .activity(dto.getAiInsights().getActivity())
                .emotionTrend(dto.getAiInsights().getEmotionTrend())
                .specialMoment(dto.getAiInsights().getSpecialMoment())
                .digestSummary(dto.getDigestSummary())  // Digest 전체 요약
                .build();
        digest = digestRepository.save(digest);

        // 2) 각 Entry 처리
        for (DigestResponseEntryDto entryDto : dto.getEntries()) {
            UUID diaryId = entryDto.getDiaryId();
            Diary diary = diaryRepository.findById(diaryId)
                    .orElseThrow(() -> new EntityNotFoundException("Diary not found: " + diaryId));

            // (선택) Diary 자체의 요약 필드 업데이트
            diary.updateOverallDaySummary(entryDto.getDiarySummary());
            diaryRepository.save(diary);

            // 중복 체크 후 DigestEntry 생성
            if (!digestEntryRepository.existsByDigestIdAndDiaryId(digest.getId(), diaryId)) {
                DigestEntry entry = DigestEntry.builder()
                        .digest(digest)
                        .diary(diary)
                        .diarySummary(entryDto.getDiarySummary())  // 여기에서 summary 매핑!
                        .build();

                // bi-directional 연관관계 유지 (선택)
                entry.updateDigest(digest);
                digest.getEntries().add(entry);

                digestEntryRepository.save(entry);
            }
        }

        return digest.getId();
    }

    /** 저장 후, 화면 조합용으로 Digest 엔티티를 가져오는 메서드 */
    @Transactional(readOnly = true)
    public Digest findDigestById(UUID digestId) {
        return digestRepository.findById(digestId)
                .orElseThrow(() -> new EntityNotFoundException("Digest not found: " + digestId));
    }

    /** userId에 해당하는 Digest 요약 정보 리스트 반환 */
    @Transactional(readOnly = true)
    public List<DigestSummaryDto> getDigestsByUser(UUID userId) {
        return digestRepository.findAllByUserId(userId).stream()
                .map(d -> {
                    DigestSummaryDto dto = new DigestSummaryDto();
                    dto.setId(d.getId());
                    dto.setTitle(d.getTitle());
                    dto.setPeriodStart(d.getPeriodStart());
                    dto.setPeriodEnd(d.getPeriodEnd());
                    dto.setSummary(d.getDigestSummary());
                    return dto;
                })
                .collect(Collectors.toList());
    }


    @Transactional(readOnly = true)
    public DigestDetailDto getDigestDetails(UUID digestId) {
        Digest d = digestRepository.findById(digestId)
                .orElseThrow(() -> new EntityNotFoundException("Digest not found: " + digestId));

        DigestDetailDto dto = new DigestDetailDto();
        dto.setId(d.getId());
        dto.setUserId(d.getUserId());
        dto.setTitle(d.getTitle());
        dto.setOverallEmotion(d.getOverallEmotion());
        dto.setPeriodStart(d.getPeriodStart());
        dto.setPeriodEnd(d.getPeriodEnd());
        dto.setActivity(d.getActivity());
        dto.setEmotionTrend(d.getEmotionTrend());
        dto.setSpecialMoment(d.getSpecialMoment());
        dto.setSummary(d.getDigestSummary());

        List<DigestEntryDetailDto> entryDtos = d.getEntries().stream()
                .map(e -> {
                    Diary diary = e.getDiary();                          // :contentReference[oaicite:1]{index=1}, :contentReference[oaicite:2]{index=2}
                    // 사진이 여러 개라면, 예시로 첫 번째 사진을 사용
                    DiaryPhoto photo = diary.getPhotos().isEmpty()
                            ? null
                            : diary.getPhotos().get(0);                      // :contentReference[oaicite:3]{index=3}

                    DigestEntryDetailDto ed = new DigestEntryDetailDto();
                    ed.setDiaryId(diary.getId());
                    ed.setCapturedAt(photo != null ? photo.getCapturedAt() : null);
                    ed.setEmotion(diary.getEmotion());
                    ed.setLatitude(photo != null ? photo.getLatitude() : null);
                    ed.setLongitude(photo != null ? photo.getLongitude() : null);
                    ed.setSummary(e.getDiarySummary());
                    return ed;
                })
                .collect(Collectors.toList());

        dto.setEntries(entryDtos);
        return dto;
    }
}
