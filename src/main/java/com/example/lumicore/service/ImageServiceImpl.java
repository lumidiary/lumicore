package com.example.lumicore.service;

import com.example.lumicore.dto.readSession.ImageData;
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
import com.oracle.bmc.queue.QueueClient;
import com.oracle.bmc.queue.model.PutMessagesDetails;
import com.oracle.bmc.queue.model.PutMessagesDetailsEntry;
import com.oracle.bmc.queue.requests.PutMessagesRequest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.Collections;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImageServiceImpl implements ImageService {

    private final ObjectStorage objectStorage;
    private final DiaryRepository diaryRepository;
    private final DiaryPhotoRepository diaryPhotoRepository;
    private final QueueClient queueClient;

    @Value("${oci.objectstorage.namespace}")
    private String namespaceName;

    @Value("${oci.objectstorage.bucket}")
    private String bucketName;

    @Value("${oci.objectstorage.uri-prefix}")
    private String uriPrefix;

    @Value("${oci.queue.id}")
    private String queueId;

    private String buildFullUri(String accessPath) {
        // accessPath 는 "/p/…/o/diary/파일명" 형태로 시작
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

        List<ImageData> images = diaryPhotoRepository.findByDiaryId(diaryId).stream()
                .map(photo -> {
                    try {
                        String rawKey = photo.getObjectKey();
                        String fullKey = rawKey.startsWith("diary/")
                                ? rawKey
                                : "diary/" + rawKey;
                        
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

                        return new ImageData(photo.getId().toString(), accessUri);

                    } catch (Exception e) {
                        log.error("READ-PAR 생성 실패: photoId={}", photo.getId(), e);
                        throw new RuntimeException(e);
                    }
                }).collect(Collectors.toList());

        return new ReadSessionResponse(diaryId.toString(), images, userLocale);
    }

    @Override
    @Transactional
    public ReadSessionResponse createAnalysisReadPar(UUID diaryId) throws Exception {
        try {
            // 다이어리 존재 여부 확인
            Diary diary = diaryRepository.findById(diaryId)
                    .orElseThrow(() -> new IllegalArgumentException("Diary not found: " + diaryId));

            // 다이어리의 모든 사진에 대한 READ-PAR 생성
            List<DiaryPhoto> photos = diaryPhotoRepository.findByDiaryId(diaryId);
            if (photos.isEmpty()) {
                log.warn("No photos found for diary: {}", diaryId);
                throw new IllegalArgumentException("No photos found for diary: " + diaryId);
            }

            // generateReadSession 메소드를 재사용하여 PAR 생성
            return generateReadSession(diaryId);
        } catch (Exception e) {
            log.error("Error while creating analysis PAR for diary: {}", diaryId, e);
            throw e;
        }
    }

    private void publishToQueue(String messageBody) {
        PutMessagesDetailsEntry entry = PutMessagesDetailsEntry.builder()
                .content(messageBody)
                .build();

        PutMessagesDetails messagesDetails = PutMessagesDetails.builder()
                .messages(Collections.singletonList(entry))
                .build();

        PutMessagesRequest request = PutMessagesRequest.builder()
                .queueId(queueId)
                .putMessagesDetails(messagesDetails)
                .build();

        queueClient.putMessages(request);
    }
}
