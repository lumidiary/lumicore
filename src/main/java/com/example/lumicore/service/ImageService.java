package com.example.lumicore.service;

import com.example.lumicore.dto.DiaryPhotoDto;
import org.jvnet.hk2.annotations.Service;
import org.springframework.web.multipart.MultipartFile;

public interface ImageService {


    DiaryPhotoDto uploadImage(MultipartFile file) throws Exception;

    //public Long uploadDiaryImage();

    //public String getDiaryImageUrl();

    //public String sendImageToAI();

}
