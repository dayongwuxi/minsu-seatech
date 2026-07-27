package com.seatech.minsu.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.seatech.minsu.common.BusinessException;
import com.seatech.minsu.common.NoGenerator;
import com.seatech.minsu.common.PageResult;
import com.seatech.minsu.common.Result;
import com.seatech.minsu.common.ResultCode;
import com.seatech.minsu.config.AuthContext;
import com.seatech.minsu.entity.Admin;
import com.seatech.minsu.entity.Notice;
import com.seatech.minsu.service.AdminService;
import com.seatech.minsu.service.NoticeService;
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

import java.time.LocalDateTime;

/** 公告管理 */
@RestController
@RequestMapping("/api/admin/notices")
@RequiredArgsConstructor
public class AdminNoticeController {

    private final NoticeService noticeService;
    private final AdminService adminService;
    private final OperationLogService operationLogService;

    @GetMapping
    public Result<PageResult<Notice>> page(@RequestParam(defaultValue = "1") long current,
                                           @RequestParam(defaultValue = "10") long size,
                                           @RequestParam(required = false) String title,
                                           @RequestParam(required = false) Integer publishStatus) {
        Page<Notice> page = noticeService.lambdaQuery()
                .like(StringUtils.hasText(title), Notice::getTitle, title)
                .eq(publishStatus != null, Notice::getPublishStatus, publishStatus)
                .orderByDesc(Notice::getCreateTime)
                .page(new Page<>(current, size));
        return Result.ok(PageResult.of(page));
    }

    @GetMapping("/{id}")
    public Result<Notice> detail(@PathVariable Long id) {
        return Result.ok(requireNotice(id));
    }

    @PostMapping
    public Result<Void> save(@RequestBody Notice notice) {
        if (!StringUtils.hasText(notice.getNoticeNo())) {
            notice.setNoticeNo(NoGenerator.gen("GG"));
        }
        if (notice.getPublishStatus() == null) {
            notice.setPublishStatus(0);
        }
        Long adminId = AuthContext.get();
        notice.setCreatorId(adminId);
        Admin admin = adminService.getById(adminId);
        if (admin != null) {
            notice.setCreatorName(admin.getName() != null ? admin.getName() : admin.getUsername());
        }
        noticeService.save(notice);
        operationLogService.record(2, adminId, "新增公告: " + notice.getTitle());
        return Result.ok();
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Notice notice) {
        requireNotice(id);
        notice.setId(id);
        noticeService.updateById(notice);
        operationLogService.record(3, AuthContext.get(), "修改公告: " + id);
        return Result.ok();
    }

    @PutMapping("/{id}/publish")
    public Result<Void> publish(@PathVariable Long id) {
        Notice notice = requireNotice(id);
        notice.setPublishStatus(1);
        notice.setPublishTime(LocalDateTime.now());
        noticeService.updateById(notice);
        operationLogService.record(3, AuthContext.get(), "发布公告: " + notice.getTitle());
        return Result.ok();
    }

    @PutMapping("/{id}/offline")
    public Result<Void> offline(@PathVariable Long id) {
        Notice notice = requireNotice(id);
        notice.setPublishStatus(0);
        noticeService.updateById(notice);
        operationLogService.record(3, AuthContext.get(), "下架公告: " + notice.getTitle());
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        noticeService.removeById(id);
        operationLogService.record(4, AuthContext.get(), "删除公告: " + id);
        return Result.ok();
    }

    private Notice requireNotice(Long id) {
        Notice notice = noticeService.getById(id);
        if (notice == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "公告不存在");
        }
        return notice;
    }
}
