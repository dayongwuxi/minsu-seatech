package com.seatech.minsu.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.seatech.minsu.common.PageResult;
import com.seatech.minsu.common.Result;
import com.seatech.minsu.config.AuthContext;
import com.seatech.minsu.dto.StatusRequest;
import com.seatech.minsu.entity.Banner;
import com.seatech.minsu.service.BannerService;
import com.seatech.minsu.service.OperationLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 轮播图管理 */
@RestController
@RequestMapping("/api/admin/banners")
@RequiredArgsConstructor
public class AdminBannerController {

    private final BannerService bannerService;
    private final OperationLogService operationLogService;

    @GetMapping
    public Result<PageResult<Banner>> page(@RequestParam(defaultValue = "1") long current,
                                           @RequestParam(defaultValue = "10") long size,
                                           @RequestParam(required = false) String title,
                                           @RequestParam(required = false) Integer status) {
        Page<Banner> page = bannerService.lambdaQuery()
                .like(StringUtils.hasText(title), Banner::getTitle, title)
                .eq(status != null, Banner::getStatus, status)
                .orderByAsc(Banner::getSort)
                .page(new Page<>(current, size));
        return Result.ok(PageResult.of(page));
    }

    @PostMapping
    public Result<Void> save(@RequestBody Banner banner) {
        bannerService.save(banner);
        operationLogService.record(2, AuthContext.get(), "新增轮播图: " + banner.getTitle());
        return Result.ok();
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Banner banner) {
        banner.setId(id);
        bannerService.updateById(banner);
        operationLogService.record(3, AuthContext.get(), "修改轮播图: " + id);
        return Result.ok();
    }

    @PutMapping("/{id}/status")
    public Result<Void> status(@PathVariable Long id, @RequestBody StatusRequest req) {
        Banner banner = new Banner();
        banner.setId(id);
        banner.setStatus(req.getStatus());
        bannerService.updateById(banner);
        operationLogService.record(3, AuthContext.get(), "修改轮播图状态: " + id);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        bannerService.removeById(id);
        operationLogService.record(4, AuthContext.get(), "删除轮播图: " + id);
        return Result.ok();
    }
}
