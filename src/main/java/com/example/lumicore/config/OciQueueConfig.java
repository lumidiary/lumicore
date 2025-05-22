package com.example.lumicore.config;

import com.example.lumicore.service.QueueService;
import com.example.lumicore.service.DigestQueueService;
import com.oracle.bmc.auth.AuthenticationDetailsProvider;
import com.oracle.bmc.auth.ConfigFileAuthenticationDetailsProvider;
import com.oracle.bmc.queue.QueueClient;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class OciQueueConfig {
    @Bean
    public AuthenticationDetailsProvider ociAuthProvider() throws Exception {
        return new ConfigFileAuthenticationDetailsProvider(
                "C:/Users/small/.oci/config", "DEFAULT"
        );
    }

    @Bean
    public QueueClient queueClient(AuthenticationDetailsProvider provider) {
        return QueueClient.builder()
                .endpoint("https://cell-1.queue.messaging.ap-chuncheon-1.oci.oraclecloud.com")
                .build(provider);
    }

}
