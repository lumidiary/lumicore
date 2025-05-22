package com.example.lumicore.dto.digest;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@Getter
@AllArgsConstructor
public class HtmlDigestResponseDto {
    private UUID userId;
    private String htmlBody;
}
