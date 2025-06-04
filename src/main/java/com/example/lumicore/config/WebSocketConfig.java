package com.example.lumicore.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.lang.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(@NonNull MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic");  // 구독 endpoint prefix
        config.setApplicationDestinationPrefixes("/app");  // 메시지 발행 endpoint prefix
    }

    @Override
    public void registerStompEndpoints(@NonNull StompEndpointRegistry registry) {
        registry.addEndpoint("/core/ws")
                .setAllowedOriginPatterns(
                        "http://localhost:[*]",        // 개발용
                        "https://lumidiary.com",       // 루트 도메인
                        "https://*.lumidiary.com",     // 서브도메인 (api.lumidiary.com 포함)
                        "https://api.lumidiary.com",   // 프론트엔드 도메인
                        "http://localhost:5173",
                        "http://localhost:8082",
                        "http://localhost:3000",
                        "https://lumi-fe-eta.vercel.app" // Vercel 배포된 프론트엔드
                )
                .withSockJS()
                .setSessionCookieNeeded(false);
    }

} 