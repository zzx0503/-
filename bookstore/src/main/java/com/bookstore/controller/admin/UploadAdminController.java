package com.bookstore.controller.admin;

import com.bookstore.anno.AdminRequired;
import com.bookstore.response.Result;
import com.bookstore.service.OSSService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "文件上传", description = "后台通用文件上传到 OSS")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@AdminRequired
public class UploadAdminController {

    private final OSSService ossService;

    @Operation(summary = "上传文件到 OSS")
    @PostMapping("/upload")
    public Result<String> upload(@RequestParam MultipartFile file) {
        String url = ossService.upload(file);
        return Result.success(url);
    }
}
