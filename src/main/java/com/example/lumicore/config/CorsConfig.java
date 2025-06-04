package com.example.lumicore.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class CorsConfig {
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of(
                "http://localhost:[*]",        // 개발용
                "https://api.lumidiary.com",   // 프론트엔드 도메인
                "http://localhost:5173",
                "http://localhost:8082",
                "http://localhost:3000",
                "https://lumidiary.com",       // 루트 도메인
                "https://*.lumidiary.com",     // 서브도메인 (api.lumidiary.com 포함)
                "https://lumi-fe-eta.vercel.app" // Vercel 배포된 프론트엔드
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of(
                "DNT","User-Agent","X-Requested-With","If-Modified-Since",
                "Cache-Control","Content-Type","Range","Authorization"
        ));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}