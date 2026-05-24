package com.bookstore.service;

import com.bookstore.domain.vo.oss.STSTokenVO;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

public interface OSSService {

    STSTokenVO getUploadToken(Long userId, String type);

    void uploadFile(String key, InputStream content, long size, String contentType);

    String upload(MultipartFile file);

    String uploadAvatar(Long userId, MultipartFile file);
}
