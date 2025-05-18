package com.example.lumicore.service;

import com.example.lumicore.dto.readSession.ReadSessionResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oracle.bmc.queue.QueueClient;
import com.oracle.bmc.queue.model.PutMessagesDetails;
import com.oracle.bmc.queue.model.PutMessagesDetailsEntry;
import com.oracle.bmc.queue.requests.PutMessagesRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@RequiredArgsConstructor
@Slf4j
public class QueueService {

    private final QueueClient queueClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${oci.queue.queue-id}")
    private String queueId;

    public void sendReadSession(ReadSessionResponse payload) {
        try {
            // 1) payload → JSON
            String json = objectMapper.writeValueAsString(payload);

            // 2) 단일 메시지 엔트리: content() 를 사용
            PutMessagesDetailsEntry entry = PutMessagesDetailsEntry.builder()
                    .content(json)
                    // .metadata(...) 필요 시 추가
                    .build();

            // 3) 묶음으로 감싼다
            PutMessagesDetails details = PutMessagesDetails.builder()
                    .messages(Collections.singletonList(entry))
                    .build();

            // 4) 요청 생성
            PutMessagesRequest request = PutMessagesRequest.builder()
                    .queueId(queueId)
                    .putMessagesDetails(details)
                    .build();

            // 5) 전송
            queueClient.putMessages(request);

            log.info("Enqueued ReadSessionResponse to OCI Queue [{}]", queueId);
        } catch (Exception e) {
            log.error("Failed to enqueue ReadSessionResponse", e);
            throw new RuntimeException("OCI Queue 전송 실패", e);
        }
    }
}
