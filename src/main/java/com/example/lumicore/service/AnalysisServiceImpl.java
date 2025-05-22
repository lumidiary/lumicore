package com.example.lumicore.service;

import com.example.lumicore.dto.question.QuestionItemDto;
import com.example.lumicore.dto.question.QuestionListResponseDto;
import com.example.lumicore.dto.analysis.AnalysisResultDto;
import com.example.lumicore.dto.analysis.ImageAnalysisDto;
import com.example.lumicore.dto.analysis.LandmarkDto;
import com.example.lumicore.jpa.entity.Diary;
import com.example.lumicore.jpa.entity.DiaryPhoto;
import com.example.lumicore.jpa.entity.Landmark;
import com.example.lumicore.jpa.entity.DiaryQA;
import com.example.lumicore.jpa.repository.DiaryPhotoRepository;
import com.example.lumicore.jpa.repository.DiaryRepository;
import com.example.lumicore.jpa.repository.LandmarkRepository;
import com.example.lumicore.jpa.repository.DiaryQARepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AnalysisServiceImpl implements AnalysisService {

    private final DiaryRepository diaryRepo;
    private final DiaryPhotoRepository photoRepo;
    private final LandmarkRepository landmarkRepo;
    private final DiaryQARepository qaRepo;

    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    @Transactional
    public QuestionListResponseDto processAnalysis(AnalysisResultDto dto) {
        UUID diaryId = null;

        List<QuestionItemDto> results = new ArrayList<>();
        // 1) 이미지별 처리
        for (ImageAnalysisDto img : dto.getImages()) {
            UUID photoId = UUID.fromString(img.getImageId());
            DiaryPhoto photo = photoRepo.findById(photoId)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid imageId: " + img.getImageId()));

            // 설명·촬영시간 갱신
            photo.updateDescription(img.getDescription());
            photo.updateCapturedAt(LocalDateTime.parse(img.getMetadata().getCaptureDate(), dtf));

            // **위도·경도 갱신** (MetadataDto에 latitude/longitude 필드가 있어야 합니다)
            photo.updateLocation(
                    img.getMetadata().getLocation().getLatitude(),
                    img.getMetadata().getLocation().getLongitude()
            );

            // 랜드마크 매핑
            for (LandmarkDto lmDto : img.getMetadata().getNearbyLandmarks()) {
                Landmark landmark = landmarkRepo.findById(lmDto.getId())
                        .orElseGet(() -> landmarkRepo.save(
                                Landmark.builder()
                                        .id(lmDto.getId())
                                        .name(lmDto.getName())
                                        .build()
                        ));
                photo.addLandmark(landmark);
            }

            photoRepo.save(photo);

            // diaryId 결정
            if (diaryId == null) {
                diaryId = photo.getDiary().getId();
            }
        }


        UUID finalDiaryId = diaryId;
        Diary diary = diaryRepo.findById(diaryId)
                .orElseThrow(() -> new EntityNotFoundException("Diary not found: " + finalDiaryId));
        diary.updateOverallDaySummary(dto.getOverallDaySummary());
        diaryRepo.save(diary);


        // 2) 질문별 DiaryQA 저장
        for (String question : dto.getQuestions()) {
            DiaryQA qa = DiaryQA.builder()
                    .diary(Diary.builder().id(diaryId).build())
                    .aiQuestion(question)
                    .userAnswer("")
                    .build();
            qaRepo.save(qa);

            results.add(new QuestionItemDto(qa.getId(), question));
        }

        // 3) 응답 DTO 반환
        return new QuestionListResponseDto(diaryId, results);
    }
}
