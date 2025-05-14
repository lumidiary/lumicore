package com.example.lumicore.service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.UUID;

import com.example.lumicore.dto.DiaryPhotoDto;
import com.example.lumicore.jpa.entity.DiaryPhoto;
import com.example.lumicore.jpa.repository.DiaryPhotoRepository;
import com.oracle.bmc.objectstorage.ObjectStorage;
import com.oracle.bmc.objectstorage.model.CreatePreauthenticatedRequestDetails;
import com.oracle.bmc.objectstorage.requests.CreatePreauthenticatedRequestRequest;
import com.oracle.bmc.objectstorage.responses.CreatePreauthenticatedRequestResponse;
import com.oracle.bmc.objectstorage.requests.PutObjectRequest;
import com.oracle.bmc.objectstorage.transfer.UploadManager;

import lombok.Generated;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * ImageServiceImpl:
 * - MultipartFile → File
 * - OCI SDK UploadManager.upload(...) 으로 업로드
 * - DiaryPhoto 엔티티 저장
 * - Pre-Authenticated Request(READ) 생성 → URL 반환
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ImageServiceImpl implements ImageService {

    private final ObjectStorage objectStorage;           // OCI ObjectStorage 클라이언트
    private final UploadManager uploadManager;           // OCI SDK UploadManager
    private final DiaryPhotoRepository diaryPhotoRepository;

    @Value("${oci.objectstorage.namespace}")
    private String namespaceName;

    @Value("${oci.objectstorage.bucket}")
    private String bucketName;

    @Value("${oci.objectstorage.uri-prefix}")
    private String defaultUriPrefix;

    @Override
    public DiaryPhotoDto uploadImage(MultipartFile multipartFile) throws Exception {
        log.info("uploadImage 시작: filename='{}', size={} bytes",
                multipartFile.getOriginalFilename(),
                multipartFile.getSize());

        // 0) MultipartFile → 임시 File 변환
        File file = File.createTempFile("upload-", "_" + multipartFile.getOriginalFilename());
        file.deleteOnExit();
        multipartFile.transferTo(file);
        log.debug("임시 파일 생성: {}", file.getAbsolutePath());

        try {
            // 1) objectKey 생성
            String objectKey = "diary/" + UUID.randomUUID() + "_" + file.getName();
            log.debug("objectKey={}", objectKey);

            // 2) OCI SDK UploadManager.upload 사용
            //    PutObjectRequest 에 namespace, bucket, objectName, contentType 지정
            PutObjectRequest putReq = PutObjectRequest.builder()
                    .namespaceName(namespaceName)
                    .bucketName(bucketName)
                    .objectName(objectKey)
                    .contentType(
                            multipartFile.getContentType() != null
                                    ? multipartFile.getContentType()
                                    : MediaType.APPLICATION_OCTET_STREAM_VALUE
                    )
                    .build();

            //    UploadRequest 에 파일과 overwrite 허용 설정
            UploadManager.UploadRequest uploadReq = UploadManager.UploadRequest.builder(file)
                    .allowOverwrite(true)
                    .build(putReq);

            //    실제 업로드 호출
            UploadManager.UploadResponse uploadResp = uploadManager.upload(uploadReq);
            log.info("UploadManager.upload 성공: etag={}, opcRequestId={}",
                    uploadResp.getETag(),
                    uploadResp.getOpcRequestId());

//            // 3) DiaryPhoto 엔티티 저장
//            DiaryPhoto diaryPhoto = new DiaryPhoto();
//            diaryPhoto.setObjectKey(objectKey);
//            diaryPhotoRepository.save(diaryPhoto);
//            log.info("DiaryPhoto 저장: id={}, objectKey={}",
//                    diaryPhoto.getId(), diaryPhoto.getObjectKey());

            // 4) 읽기용 PRE-AUTH 생성

            Date expiresAt = Date.from(
                    OffsetDateTime.now(ZoneId.systemDefault())
                            .plusHours(1)
                            .toInstant()
            );

            CreatePreauthenticatedRequestDetails readPar =
                    CreatePreauthenticatedRequestDetails.builder()
                            .name("access-" + UUID.randomUUID())
                            .objectName(objectKey)
                            .accessType(CreatePreauthenticatedRequestDetails.AccessType.ObjectRead)
                            .timeExpires(expiresAt)
                            .build();

            CreatePreauthenticatedRequestRequest readReq =
                    CreatePreauthenticatedRequestRequest.builder()
                            .namespaceName(namespaceName)
                            .bucketName(bucketName)
                            .createPreauthenticatedRequestDetails(readPar)
                            .build();

            CreatePreauthenticatedRequestResponse readResp =
                    objectStorage.createPreauthenticatedRequest(readReq);

            String accessUri = defaultUriPrefix +
                    readResp.getPreauthenticatedRequest().getAccessUri();
            log.debug("READ-PAR URL={}", accessUri);

            // 5) DTO 반환
            DiaryPhotoDto dto = new DiaryPhotoDto();
            dto.setObjectKey(objectKey);
            dto.setAccessUri(accessUri);
            log.info("uploadImage 완료");
            return dto;

        } finally {
            // finally 블록에서 강제 삭제 시도
            try {
                Path path = file.toPath();
                if (Files.deleteIfExists(path)) {
                    log.debug("임시 파일 강제 삭제 성공: {}", path);
                } else {
                    log.warn("임시 파일이 존재하지 않거나 삭제 실패: {}", path);
                }
            } catch (Exception ex) {
                log.error("임시 파일 삭제 중 예외 발생: {}", file.getAbsolutePath(), ex);
            }
        }
    }
}
