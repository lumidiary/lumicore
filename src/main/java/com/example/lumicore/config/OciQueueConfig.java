package com.example.lumicore.config;

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

    @Value("${oci.configFilePath}")
    private String configFilePath;

    @Bean
    public AuthenticationDetailsProvider ociAuthProvider() throws Exception {
        return new ConfigFileAuthenticationDetailsProvider(
                configFilePath, "DEFAULT"
        );
    }

    @Bean
    public QueueClient queueClient(AuthenticationDetailsProvider provider) {
        return QueueClient.builder()
                .endpoint("https://cell-1.queue.messaging.ap-chuncheon-1.oci.oraclecloud.com")
                .build(provider);
    }
}
