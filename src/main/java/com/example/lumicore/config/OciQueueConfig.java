package com.example.lumicore.config;

import com.oracle.bmc.auth.AuthenticationDetailsProvider;
import com.oracle.bmc.auth.ConfigFileAuthenticationDetailsProvider;
import com.oracle.bmc.queue.QueueClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OciQueueConfig {

    @Bean
    public AuthenticationDetailsProvider ociAuthProvider() throws Exception {
        // ~/.oci/config 의 DEFAULT 프로파일 사용
        return new ConfigFileAuthenticationDetailsProvider("C:/Users/small/.oci/config", "DEFAULT");
    }

    @Bean
    public QueueClient queueClient(AuthenticationDetailsProvider provider) {
        return QueueClient.builder()
                .endpoint("https://cell-1.queue.messaging.ap-chuncheon-1.oci.oraclecloud.com")
                .build(provider);
    }

}
