package com.example.lumicore.dto.digest.request;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DigestRequestDto {

    private String id;
    private List<DigestRequestEntryDto> entries;
    private String userLocale;
}
