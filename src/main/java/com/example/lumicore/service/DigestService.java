package com.example.lumicore.service;

import com.example.lumicore.dto.digest.DigestDetailDto;
import com.example.lumicore.dto.digest.DigestEntryDetailDto;
import com.example.lumicore.dto.digest.DigestSummaryDto;
import com.example.lumicore.dto.digest.response.DigestResponseDto;
import com.example.lumicore.dto.digest.response.DigestResponseEntryDto;
import com.example.lumicore.dto.readSession.ReadSessionResponse;
import com.example.lumicore.jpa.entity.Diary;
import com.example.lumicore.jpa.entity.DiaryPhoto;
import com.example.lumicore.jpa.entity.Digest;
import com.example.lumicore.jpa.entity.DigestEntry;
import com.example.lumicore.jpa.repository.DiaryRepository;
import com.example.lumicore.jpa.repository.DigestEntryRepository;
import com.example.lumicore.jpa.repository.DigestRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class DigestService {

    private final DigestRepository digestRepository;
    private final DiaryRepository diaryRepository;
    private final DigestEntryRepository digestEntryRepository;
    private final ImageService imageService;

    @Transactional
    public UUID saveDigestFromResponse(DigestResponseDto dto) {
        log.info("Received DigestResponseDto: id={}, title={}, entries count={}", 
                dto.getId(), dto.getTitle(), 
                dto.getEntries() != null ? dto.getEntries().size() : 0);
        
        // Null validation for required fields
        if (dto.getId() == null || dto.getId().trim().isEmpty()) {
            log.error("User ID is null or empty in DigestResponseDto");
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }
        if (dto.getPeriod() == null) {
            throw new IllegalArgumentException("Period cannot be null");
        }
        if (dto.getAiInsights() == null) {
            throw new IllegalArgumentException("AI Insights cannot be null");
        }
        
        // 1) Digest 생성 및 저장
        LocalDate startDate = LocalDate.parse(dto.getPeriod().getStart());
        LocalDate endDate = LocalDate.parse(dto.getPeriod().getEnd());
        Digest digest = Digest.builder()
                .userId(UUID.fromString(dto.getId()))
                .periodStart(startDate)
                .periodEnd(endDate)
                .title(dto.getTitle())
                .overallEmotion(dto.getOverallEmotion())
                .activity(dto.getAiInsights().getActivity())
                .emotionTrend(dto.getAiInsights().getEmotionTrend())
                .specialMoment(dto.getAiInsights().getSpecialMoment())
                .digestSummary(dto.getDigestSummary())  // Digest 전체 요약
                .build();
        digest = digestRepository.save(digest);

        // 2) 각 Entry 처리
        if (dto.getEntries() != null) {
            for (DigestResponseEntryDto entryDto : dto.getEntries()) {
                if (entryDto.getDiaryId() == null || entryDto.getDiaryId().trim().isEmpty()) {
                    continue; // Skip invalid entries
                }
                UUID diaryId = UUID.fromString(entryDto.getDiaryId());
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
                    
                    // 첫번째 엔트리의 첫번째 포토에서 READ PAR URL 생성
                    String imageUrl = null;
                    if (!d.getEntries().isEmpty()) {
                        DigestEntry firstEntry = d.getEntries().get(0);
                        Diary diary = firstEntry.getDiary();
                        if (!diary.getPhotos().isEmpty()) {
                            try {
                                ReadSessionResponse session = imageService.generateReadSession(diary.getId());
                                if (!session.getImages().isEmpty()) {
                                    imageUrl = session.getImages().get(0).getUrl();
                                }
                            } catch (Exception e) {
                                // 이미지 URL 생성 실패시 null로 유지
                                imageUrl = null;
                            }
                        }
                    }
                    dto.setImageUrl(imageUrl);
                    
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
                    Diary diary = e.getDiary();
                    // 사진이 여러 개라면, 예시로 첫 번째 사진을 사용
                    DiaryPhoto photo = diary.getPhotos().isEmpty()
                            ? null
                            : diary.getPhotos().get(0);

                    DigestEntryDetailDto ed = new DigestEntryDetailDto();
                    ed.setDiaryId(diary.getId());
                    ed.setCapturedAt(photo != null ? photo.getCapturedAt() : null);
                    ed.setEmotion(diary.getEmotion());
                    ed.setLatitude(photo != null ? photo.getLatitude() : null);
                    ed.setLongitude(photo != null ? photo.getLongitude() : null);
                    ed.setSummary(e.getDiarySummary());
                    
                    // 첫번째 포토의 READ PAR URL 생성
                    String imageUrl = null;
                    if (photo != null) {
                        try {
                            ReadSessionResponse session = imageService.generateReadSession(diary.getId());
                            if (!session.getImages().isEmpty()) {
                                // 해당 photo의 URL을 찾거나 첫번째 URL 사용
                                imageUrl = session.getImages().stream()
                                        .filter(imageData -> imageData.getId().equals(photo.getId().toString()))
                                        .findFirst()
                                        .orElse(session.getImages().get(0))
                                        .getUrl();
                            }
                        } catch (Exception ex) {
                            // 이미지 URL 생성 실패시 null로 유지
                            imageUrl = null;
                        }
                    }
                    ed.setImageUrl(imageUrl);
                    
                    return ed;
                })
                .collect(Collectors.toList());

        dto.setEntries(entryDtos);
        return dto;
    }
}