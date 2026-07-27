package com.seatech.minsu.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.seatech.minsu.common.BusinessException;
import com.seatech.minsu.common.PageResult;
import com.seatech.minsu.common.Result;
import com.seatech.minsu.common.ResultCode;
import com.seatech.minsu.entity.Banner;
import com.seatech.minsu.entity.Notice;
import com.seatech.minsu.entity.Room;
import com.seatech.minsu.service.BannerService;
import com.seatech.minsu.service.NoticeService;
import com.seatech.minsu.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 用户端首页：轮播图/推荐房间/公告 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class HomeController {

    private final BannerService bannerService;
    private final RoomService roomService;
    private final NoticeService noticeService;

    @GetMapping("/banners")
    public Result<List<Banner>> banners() {
        return Result.ok(bannerService.lambdaQuery()
                .eq(Banner::getStatus, 1)
                .orderByAsc(Banner::getSort)
                .list());
    }

    @GetMapping("/rooms/recommend")
    public Result<List<Room>> recommendRooms() {
        return Result.ok(roomService.lambdaQuery()
                .eq(Room::getIsRecommend, 1)
                .eq(Room::getShelfStatus, 1)
                .orderByDesc(Room::getCreateTime)
                .last("LIMIT 6")
                .list());
    }

    @GetMapping("/notices")
    public Result<PageResult<Notice>> notices(@RequestParam(defaultValue = "1") long current,
                                              @RequestParam(defaultValue = "10") long size) {
        Page<Notice> page = noticeService.lambdaQuery()
                .eq(Notice::getPublishStatus, 1)
                .orderByDesc(Notice::getPublishTime)
                .page(new Page<>(current, size));
        return Result.ok(PageResult.of(page));
    }

    @GetMapping("/notices/{id}")
    public Result<Notice> noticeDetail(@PathVariable Long id) {
        Notice notice = noticeService.getById(id);
        if (notice == null || notice.getPublishStatus() == null || notice.getPublishStatus() != 1) {
            throw new BusinessException(ResultCode.NOT_FOUND, "公告不存在或未发布");
        }
        return Result.ok(notice);
    }
}
