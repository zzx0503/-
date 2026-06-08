package com.bookstore.controller.admin;

import com.bookstore.anno.AdminRequired;
import com.bookstore.domain.vo.admin.BookCoverInitVO;
import com.bookstore.response.Result;
import com.bookstore.service.AdminBookCoverService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "图书封面管理", description = "后台批量初始化书本封面到 OSS")
@RestController
@RequestMapping("/api/admin/book-covers")
@RequiredArgsConstructor
@AdminRequired
public class BookCoverAdminController {

    private final AdminBookCoverService adminBookCoverService;

    @Operation(summary = "批量上传本地图片到 OSS 并按 id 哈希分配到 book.cover_key")
    @PostMapping("/bulk-init")
    public Result<BookCoverInitVO> bulkInit(@RequestParam(required = false) String localDir) {
        return Result.success(adminBookCoverService.bulkInit(localDir));
    }
}
