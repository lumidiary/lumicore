package com.example.lumicore.dto.digest.request;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DigestRequestDto {

    private UUID userId;
    private String userLocale;

    private List<DigestRequestEntryDto> entries;
}
