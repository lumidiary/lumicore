package com.example.lumicore.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // CSRF 보호가 불필요하면 끄고 (Form-Data 업로드 시 편리하게)
                .csrf(csrf -> csrf.disable())

                // 인증/인가 설정
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())


                // 기본 HTTP Basic 활성화 (로그인 폼 없이 브라우저 팝업 방식)
                .httpBasic(Customizer.withDefaults());

        return http.build();
    }
}
