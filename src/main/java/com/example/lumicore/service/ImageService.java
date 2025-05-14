package com.example.lumicore.service;

import com.example.lumicore.dto.readSession.ReadSessionResponse;
import com.example.lumicore.dto.uploadSession.UploadParRequest;
import com.example.lumicore.dto.uploadSession.UploadSessionResponse;

import java.util.UUID;

public interface ImageService {

    UploadSessionResponse startUploadSession(UploadParRequest request) throws Exception;

    ReadSessionResponse generateReadSession(UUID diaryId) throws Exception;

    //public Long uploadDiaryImage();

    //public String getDiaryImageUrl();

    //public String sendImageToAI();

}
