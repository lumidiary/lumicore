package com.example.lumicore.service;

import com.example.lumicore.dto.readSession.ReadParDto;
import com.example.lumicore.dto.readSession.ReadSessionResponse;
import com.example.lumicore.dto.uploadSession.UploadParDto;
import com.example.lumicore.dto.uploadSession.UploadParRequest;
import com.example.lumicore.dto.uploadSession.UploadSessionResponse;
import com.example.lumicore.jpa.entity.Diary;
import com.example.lumicore.jpa.entity.DiaryPhoto;
import com.example.lumicore.jpa.repository.DiaryPhotoRepository;
import com.example.lumicore.jpa.repository.DiaryRepository;
import com.oracle.bmc.objectstorage.ObjectStorage;
import com.oracle.bmc.objectstorage.model.CreatePreauthenticatedRequestDetails;
import com.oracle.bmc.objectstorage.requests.CreatePreauthenticatedRequestRequest;
import com.oracle.bmc.objectstorage.responses.CreatePreauthenticatedRequestResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImageServiceImpl implements ImageService {

    private final ObjectStorage objectStorage;
    private final DiaryRepository diaryRepository;
    private final DiaryPhotoRepository diaryPhotoRepository;

    @Value("${oci.objectstorage.namespace}")
    private String namespaceName;
    @Value("${oci.objectstorage.bucket}")
    private String bucketName;
    @Value("${oci.objectstorage.uri-prefix}")
    private String uriPrefix;

    /**
     * 1) Diary 생성 및
     * 2) 각 사진마다 Pre-authenticated Request(PAR) 생성
     * 3) DiaryPhoto row 생성
     *
     * @param request
     * @return
     */
    @Override
    public UploadSessionResponse startUploadSession(UploadParRequest request) throws Exception {
        // Diary 생성 및 저장 (한 번만 할당)
        final Diary diary = diaryRepository.save(Diary.builder().build());
        UUID diaryId = diary.getId();
        log.info("새 Diary 생성: diaryId={}", diaryId);

        Date expiresAt = Date.from(
                OffsetDateTime.now(ZoneId.systemDefault())
                        .plusHours(1)
                        .toInstant()
        );

        // 파일명마다 PAR 생성 & DiaryPhoto row 생성
        List<UploadParDto> pars = request.getFileNames().stream().map(name -> {
            try {
                // objectKey 생성
                String objectKey = "diary/" + UUID.randomUUID() + "_" + name;

                // PRE-AUTH 생성 로직
                CreatePreauthenticatedRequestDetails details =
                        CreatePreauthenticatedRequestDetails.builder()
                                .name("upload-" + UUID.randomUUID())
                                .objectName(objectKey)
                                .accessType(CreatePreauthenticatedRequestDetails.AccessType.ObjectWrite)
                                .timeExpires(expiresAt)
                                .build();

                CreatePreauthenticatedRequestRequest parReq =
                        CreatePreauthenticatedRequestRequest.builder()
                                .namespaceName(namespaceName)
                                .bucketName(bucketName)
                                .createPreauthenticatedRequestDetails(details)
                                .build();

                CreatePreauthenticatedRequestResponse parResp =
                        objectStorage.createPreauthenticatedRequest(parReq);

                String uploadUri = uriPrefix + parResp.getPreauthenticatedRequest().getAccessUri();
                log.debug("WRITE-PAR 생성: {} → {}", name, uploadUri);

                // DiaryPhoto 엔티티 생성 (effectively final diary 사용)
                DiaryPhoto photo = DiaryPhoto.of(diary, objectKey);
                diaryPhotoRepository.save(photo);
                log.debug("DiaryPhoto row 생성: id={}, objectKey={}", photo.getId(), objectKey);

                return new UploadParDto(photo.getId(), uploadUri);

            } catch (Exception e) {
                log.error("PAR 생성 실패: {}", name, e);
                throw new RuntimeException(e);
            }
        }).collect(Collectors.toList());

        return new UploadSessionResponse(diaryId, pars);
    }


    @Override
    public ReadSessionResponse generateReadSession(UUID diaryId) throws Exception {
        Diary diary = diaryRepository.findById(diaryId)
                .orElseThrow(() -> new IllegalArgumentException("Diary not found: " + diaryId));

        String userLocale = diary.getUserLocale();
        Date expiresAt = Date.from(
                OffsetDateTime.now(ZoneId.systemDefault()).plusHours(1).toInstant()
        );

        List<ReadParDto> items = diaryPhotoRepository.findByDiaryId(diaryId).stream()
                .map(photo -> {
                    try {
                        CreatePreauthenticatedRequestDetails rd = CreatePreauthenticatedRequestDetails.builder()
                                .name("read-" + UUID.randomUUID())
                                .objectName(photo.getObjectKey())
                                .accessType(CreatePreauthenticatedRequestDetails.AccessType.ObjectRead)
                                .timeExpires(expiresAt)
                                .build();

                        CreatePreauthenticatedRequestRequest req = CreatePreauthenticatedRequestRequest.builder()
                                .namespaceName(namespaceName)
                                .bucketName(bucketName)
                                .createPreauthenticatedRequestDetails(rd)
                                .build();

                        CreatePreauthenticatedRequestResponse resp =
                                objectStorage.createPreauthenticatedRequest(req);

                        String accessUri = uriPrefix + resp.getPreauthenticatedRequest().getAccessUri();
                        return new ReadParDto(photo.getId(), accessUri);

                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }).collect(Collectors.toList());

        return new ReadSessionResponse(diaryId, userLocale, items);
    }

}
