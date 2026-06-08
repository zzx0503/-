package com.bookstore.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.bookstore.config.OSSProperties;
import com.bookstore.domain.po.Book;
import com.bookstore.domain.vo.admin.BookCoverInitVO;
import com.bookstore.exception.BusinessException;
import com.bookstore.mapper.BookMapper;
import com.bookstore.response.ResultCode;
import com.bookstore.service.AdminBookCoverService;
import com.bookstore.service.OSSService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminBookCoverServiceImpl implements AdminBookCoverService {

    private final OSSProperties ossProps;
    private final OSSService ossService;
    private final BookMapper bookMapper;

    @Override
    public BookCoverInitVO bulkInit(String localDir) {
        BookCoverInitVO vo = new BookCoverInitVO();
        vo.setUploaded(0);
        vo.setBooksUpdated(0);

        String dir = StringUtils.hasText(localDir) ? localDir : ossProps.getBookCoverSourceDir();
        if (!StringUtils.hasText(dir)) {
            throw new BusinessException(ResultCode.BIZ_ERROR,
                "请通过 ?localDir=... 指定本地图片目录,或配置 bookstore.oss.book-cover-source-dir");
        }

        Path root = Paths.get(dir);
        if (!Files.isDirectory(root)) {
            throw new BusinessException(ResultCode.BIZ_ERROR, "目录不存在或不可读: " + dir);
        }

        List<Path> images = listImages(root);
        if (images.isEmpty()) {
            throw new BusinessException(ResultCode.BIZ_ERROR, "目录下没有图片文件: " + dir);
        }

        String coverDir = ossProps.getBookCoverDir();
        if (coverDir == null) coverDir = "covers/";
        if (!coverDir.endsWith("/")) coverDir = coverDir + "/";

        List<String> keys = new ArrayList<>();
        for (Path img : images) {
            String fileName = img.getFileName().toString();
            String key = coverDir + fileName;
            String contentType = guessContentType(fileName);
            try (InputStream in = new BufferedInputStream(new FileInputStream(img.toFile()))) {
                long size = Files.size(img);
                ossService.uploadFile(key, in, size, contentType);
                keys.add(key);
                vo.getUploadedKeys().add(key);
            } catch (Exception e) {
                String err = fileName + ": " + e.getMessage();
                log.error("upload failed: {}", err, e);
                vo.getErrors().add(err);
            }
        }
        vo.setUploaded(keys.size());

        if (keys.isEmpty()) {
            return vo;
        }

        List<Book> books = bookMapper.selectList(
            new LambdaQueryWrapper<Book>()
                .select(Book::getId)
                .eq(Book::getDeleted, 0)
        );
        if (books.isEmpty()) {
            return vo;
        }

        int count = keys.size();
        List<List<Long>> groups = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            groups.add(new ArrayList<>());
        }
        for (Book b : books) {
            int idx = (int) Math.floorMod(b.getId(), count);
            groups.get(idx).add(b.getId());
        }

        int totalUpdated = 0;
        for (int i = 0; i < count; i++) {
            List<Long> ids = groups.get(i);
            if (ids.isEmpty()) continue;
            String key = keys.get(i);
            int affected = bookMapper.update(null,
                new LambdaUpdateWrapper<Book>()
                    .set(Book::getCoverKey, key)
                    .in(Book::getId, ids)
            );
            totalUpdated += affected;
        }
        vo.setBooksUpdated(totalUpdated);
        return vo;
    }

    private List<Path> listImages(Path root) {
        List<Path> result = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(root)) {
            for (Path p : stream) {
                if (!Files.isRegularFile(p)) continue;
                String name = p.getFileName().toString().toLowerCase();
                if (name.endsWith(".jpg") || name.endsWith(".jpeg")
                    || name.endsWith(".png") || name.endsWith(".webp")
                    || name.endsWith(".gif")) {
                    result.add(p);
                }
            }
        } catch (Exception e) {
            throw new BusinessException(ResultCode.BIZ_ERROR, "读取目录失败: " + e.getMessage());
        }
        Collections.sort(result, (a, b) -> a.getFileName().toString().compareTo(b.getFileName().toString()));
        return result;
    }

    private String guessContentType(String fileName) {
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".gif")) return "image/gif";
        return "application/octet-stream";
    }
}
