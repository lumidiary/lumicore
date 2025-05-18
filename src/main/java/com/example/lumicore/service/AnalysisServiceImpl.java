package com.example.lumicore.service;

import com.example.lumicore.dto.QuestionListResponseDto;
import com.example.lumicore.dto.analysis.*;
import com.example.lumicore.jpa.entity.*;
import com.example.lumicore.jpa.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AnalysisServiceImpl implements AnalysisService {

    private final DiaryPhotoRepository photoRepo;
    private final LandmarkRepository landmarkRepo;
    private final DiaryQARepository qaRepo;

    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    @Transactional
    public QuestionListResponseDto processAnalysis(AnalysisResultDto dto) {
        UUID diaryId = null;

        // 1) 이미지별 처리
        for (ImageAnalysisDto img : dto.getImages()) {
            UUID photoId = UUID.fromString(img.getImageId());
            DiaryPhoto photo = photoRepo.findById(photoId)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid imageId: " + img.getImageId()));

            // 업데이트
            photo.updateDescription(img.getDescription());
            photo.updateCapturedAt(LocalDateTime.parse(img.getMetadata().getCaptureDate(), dtf));

            // 랜드마크 매핑
            for (LandmarkDto lmDto : img.getMetadata().getNearbyLandmarks()) {
                Landmark landmark = landmarkRepo.findById(lmDto.getId())
                        .orElseGet(() -> landmarkRepo.save(
                                Landmark.builder()
                                        .id(lmDto.getId())
                                        .name(lmDto.getName())
                                        .build()));
                photo.addLandmark(landmark);
            }

            photoRepo.save(photo);

            // diaryId 결정
            if (diaryId == null) {
                diaryId = photo.getDiary().getId();
            }
        }

        // 2) 질문별 DiaryQA 저장
        for (String question : dto.getQuestions()) {
            DiaryQA qa = DiaryQA.builder()
                    .diary(Diary.builder().id(diaryId).build())
                    .aiQuestion(question)
                    .userAnswer("")  // 초기값
                    .build();
            qaRepo.save(qa);
        }

        // 3) 응답 DTO 반환
        return new QuestionListResponseDto(diaryId, dto.getQuestions());
    }
}
