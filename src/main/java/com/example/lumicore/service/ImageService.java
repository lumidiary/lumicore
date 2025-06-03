package com.example.lumicore.service;

import com.example.lumicore.dto.readSession.ReadSessionResponse;
import com.example.lumicore.dto.uploadSession.UploadParRequest;
import com.example.lumicore.dto.uploadSession.UploadSessionResponse;

import java.util.UUID;

public interface ImageService {

    UploadSessionResponse startUploadSession(UploadParRequest request) throws Exception;

    ReadSessionResponse generateReadSession(UUID diaryId) throws Exception;

    /** WebSocket 연결 시 분석용 READ-PAR 생성 */
    ReadSessionResponse createAnalysisReadPar(UUID diaryId) throws Exception;
}
