package com.example.lumicore.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic");  // 구독 endpoint prefix
        config.setApplicationDestinationPrefixes("/app");  // 메시지 발행 endpoint prefix
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 다이어리 전용 엔드포인트
        registry.addEndpoint("/diary")
               .setAllowedOriginPatterns("http://localhost:[*]")
               .withSockJS();

        // 기존 코어 엔드포인트
        registry.addEndpoint("/core/ws")
               .setAllowedOriginPatterns("http://localhost:[*]")
               .withSockJS();
    }
} 