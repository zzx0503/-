package com.bookstore.controller.admin;

import com.bookstore.anno.AdminRequired;
import com.bookstore.domain.vo.admin.DashboardVO;
import com.bookstore.response.Result;
import com.bookstore.service.AdminStatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "后台统计", description = "管理端仪表盘")
@RestController
@RequestMapping("/api/admin/stats")
@RequiredArgsConstructor
@AdminRequired
public class AdminStatsController {

    private final AdminStatsService adminStatsService;

    @Operation(summary = "查询仪表盘数据")
    @GetMapping("/dashboard")
    public Result<DashboardVO> dashboard() {
        return Result.success(adminStatsService.dashboard());
    }
}
