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

    private String buildFullUri(String accessPath) {
        // accessPath 는 “/p/…/o/diary/파일명” 형태로 시작
        if (uriPrefix.endsWith("/")) {
            return uriPrefix.substring(0, uriPrefix.length() - 1) + accessPath;
        }
        return uriPrefix + accessPath;
    }

    @Override
    public UploadSessionResponse startUploadSession(UploadParRequest request) throws Exception {
        Diary diary = diaryRepository.save(Diary.builder().build());
        UUID diaryId = diary.getId();
        log.info("새 Diary 생성: diaryId={}", diaryId);

        Date expiresAt = Date.from(
                OffsetDateTime.now(ZoneId.systemDefault())
                        .plusHours(1)
                        .toInstant()
        );

        List<UploadParDto> pars = request.getFileNames().stream().map(name -> {
            try {
                String objectKey = "diary/" + UUID.randomUUID() + "_" + name;
                log.debug("WRITE-PAR 준비: objectKey={}", objectKey);

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

                String uploadUri = buildFullUri(
                        parResp.getPreauthenticatedRequest().getAccessUri()
                );
                log.debug("WRITE-PAR 생성: name={} → uploadUri={}", name, uploadUri);

                DiaryPhoto photo = DiaryPhoto.of(diary, objectKey);
                diaryPhotoRepository.save(photo);
                log.debug("DiaryPhoto row 생성: photoId={}, objectKey={}", photo.getId(), objectKey);

                return new UploadParDto(photo.getId(), uploadUri);

            } catch (Exception e) {
                log.error("WRITE-PAR 생성 실패: {}", name, e);
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
                OffsetDateTime.now(ZoneId.systemDefault())
                        .plusHours(1)
                        .toInstant()
        );

        List<ReadParDto> items = diaryPhotoRepository.findByDiaryId(diaryId).stream()
                .map(photo -> {
                    try {
                        // DB에 저장된 rawKey
                        String rawKey = photo.getObjectKey();
                        // diary/ 접두사가 없다면 보정
                        String fullKey = rawKey.startsWith("diary/")
                                ? rawKey
                                : "diary/" + rawKey;
                        log.debug("READ-PAR 준비: photoId={} rawKey={} → fullKey={}",
                                photo.getId(), rawKey, fullKey);

                        CreatePreauthenticatedRequestDetails details =
                                CreatePreauthenticatedRequestDetails.builder()
                                        .name("read-" + UUID.randomUUID())
                                        .objectName(fullKey)
                                        .accessType(CreatePreauthenticatedRequestDetails.AccessType.ObjectRead)
                                        .timeExpires(expiresAt)
                                        .build();

                        CreatePreauthenticatedRequestRequest req =
                                CreatePreauthenticatedRequestRequest.builder()
                                        .namespaceName(namespaceName)
                                        .bucketName(bucketName)
                                        .createPreauthenticatedRequestDetails(details)
                                        .build();

                        CreatePreauthenticatedRequestResponse resp =
                                objectStorage.createPreauthenticatedRequest(req);

                        String accessUri = buildFullUri(
                                resp.getPreauthenticatedRequest().getAccessUri()
                        );
                        log.debug("READ-PAR 생성: photoId={} → accessUri={}", photo.getId(), accessUri);

                        return new ReadParDto(photo.getId(), accessUri);

                    } catch (Exception e) {
                        log.error("READ-PAR 생성 실패: photoId={}", photo.getId(), e);
                        throw new RuntimeException(e);
                    }
                }).collect(Collectors.toList());

        return new ReadSessionResponse(diaryId, userLocale, items);
    }
}
