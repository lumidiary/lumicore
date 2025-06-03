package com.example.lumicore.service;

import com.example.lumicore.dto.readSession.ReadSessionResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oracle.bmc.queue.QueueClient;
import com.oracle.bmc.queue.model.MessageMetadata;
import com.oracle.bmc.queue.model.PutMessagesDetails;
import com.oracle.bmc.queue.model.PutMessagesDetailsEntry;
import com.oracle.bmc.queue.requests.PutMessagesRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@Slf4j
@RequiredArgsConstructor
public class QueueService {

    private final QueueClient  queueClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // field injection (non-final) 으로 프로퍼티 바인딩
    @Value("${oci.queue.id}")
    String queueId;

    @Value("${oci.queue.channels.default}")
    String channelId;

    public void sendReadSession(ReadSessionResponse payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);

            PutMessagesDetailsEntry entry = PutMessagesDetailsEntry.builder()
                    .content(json)
                    .metadata(
                            MessageMetadata.builder()
                                    .channelId(channelId)
                                    .build()
                    )
                    .build();

            PutMessagesDetails details = PutMessagesDetails.builder()
                    .messages(Collections.singletonList(entry))
                    .build();

            PutMessagesRequest req = PutMessagesRequest.builder()
                    .queueId(queueId)
                    .putMessagesDetails(details)
                    .build();

            queueClient.putMessages(req);
            log.info("Enqueued ReadSessionResponse on channel=[{}]", channelId);

        } catch (Exception e) {
            log.error("Failed to enqueue ReadSessionResponse", e);
            throw new RuntimeException(e);
        }
    }
}
