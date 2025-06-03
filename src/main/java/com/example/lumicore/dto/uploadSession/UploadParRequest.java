package com.example.lumicore.dto.uploadSession;

import lombok.Data;

import java.util.List;

@Data
public class UploadParRequest {
    private List<String> fileNames;
}
