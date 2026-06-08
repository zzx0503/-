package com.bookstore.controller;

import com.bookstore.context.UserContext;
import com.bookstore.domain.vo.oss.STSTokenVO;
import com.bookstore.response.Result;
import com.bookstore.service.OSSService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "文件上传", description = "OSS STS 临时上传凭证")
@RestController
@RequestMapping("/api/oss")
@RequiredArgsConstructor
public class OSSController {

    private final OSSService ossService;

    @Operation(summary = "获取 STS 上传凭证")
    @GetMapping("/sts-token")
    public Result<STSTokenVO> getStsToken(@RequestParam String type) {
        Long userId = UserContext.requireUserId();
        return Result.success(ossService.getUploadToken(userId, type));
    }
}
