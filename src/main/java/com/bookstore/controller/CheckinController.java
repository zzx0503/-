package com.bookstore.controller;

import com.bookstore.anno.LoginRequired;
import com.bookstore.context.UserContext;
import com.bookstore.domain.vo.checkin.CheckinRecordVO;
import com.bookstore.domain.vo.checkin.CheckinResultVO;
import com.bookstore.domain.vo.checkin.CheckinStatusVO;
import com.bookstore.response.PageResult;
import com.bookstore.response.Result;
import com.bookstore.service.CheckinService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "签到", description = "每日签到与连续奖励")
@RestController
@RequestMapping("/api/checkin")
@RequiredArgsConstructor
@LoginRequired
public class CheckinController {

    private final CheckinService checkinService;

    @PostMapping
    public Result<CheckinResultVO> checkin() {
        return Result.success(checkinService.checkin(UserContext.requireUserId()));
    }

    @GetMapping("/status")
    public Result<CheckinStatusVO> status() {
        return Result.success(checkinService.getStatus(UserContext.requireUserId()));
    }

    @GetMapping("/history")
    public Result<PageResult<CheckinRecordVO>> history(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "30") Integer size) {
        return Result.success(checkinService.getHistory(UserContext.requireUserId(), page, size));
    }
}
