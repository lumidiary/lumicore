package com.example.lumicore.service;

import com.example.lumicore.dto.digest.request.DigestRequestDto;
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

import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
@Slf4j
@RequiredArgsConstructor
public class DigestQueueService {

    private final QueueClient   queueClient;
    private final ObjectMapper  objectMapper;

    @Value("${oci.queue.queue-id}")
    private String queueId;

    @Value("${oci.queue.channels.digest}")
    private String channelId;

    /**
     * Iterable 단위로 받은 DigestRequestDto 목록을
     * OCI Queue의 'digest' 채널로 전송합니다.
     */
    public void sendDigestRequests(Iterable<? extends DigestRequestDto> items) {
        try {
            var entries = StreamSupport.stream(items.spliterator(), false)
                    .map(dto -> {
                        try {
                            String body = objectMapper.writeValueAsString(dto);
                            return PutMessagesDetailsEntry.builder()
                                    .content(body)
                                    .metadata(
                                            MessageMetadata.builder()
                                                    .channelId(channelId)
                                                    .build()
                                    )
                                    .build();
                        } catch (Exception ex) {
                            throw new RuntimeException("Failed to serialize DigestRequestDto", ex);
                        }
                    })
                    .collect(Collectors.toList());

            var details = PutMessagesDetails.builder()
                    .messages(entries)
                    .build();

            queueClient.putMessages(
                    PutMessagesRequest.builder()
                            .queueId(queueId)
                            .putMessagesDetails(details)
                            .build()
            );

            log.info("Enqueued {} Digest items on channel=[{}]", entries.size(), channelId);
        } catch (Exception e) {
            log.error("Failed to enqueue Digest items", e);
            throw new RuntimeException("OCI Queue 전송 실패", e);
        }
    }
}
