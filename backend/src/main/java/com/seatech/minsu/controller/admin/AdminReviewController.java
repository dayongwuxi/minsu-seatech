package com.seatech.minsu.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.seatech.minsu.common.BusinessException;
import com.seatech.minsu.common.PageResult;
import com.seatech.minsu.common.Result;
import com.seatech.minsu.common.ResultCode;
import com.seatech.minsu.config.AuthContext;
import com.seatech.minsu.dto.AuditRequest;
import com.seatech.minsu.dto.ReplyRequest;
import com.seatech.minsu.dto.ReviewAdminVO;
import com.seatech.minsu.dto.ReviewQuery;
import com.seatech.minsu.entity.Review;
import com.seatech.minsu.mapper.ReviewMapper;
import com.seatech.minsu.service.OperationLogService;
import com.seatech.minsu.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/** 评价记录管理 */
@RestController
@RequestMapping("/api/admin/reviews")
@RequiredArgsConstructor
public class AdminReviewController {

    private final ReviewService reviewService;
    private final ReviewMapper reviewMapper;
    private final OperationLogService operationLogService;

    @GetMapping
    public Result<PageResult<ReviewAdminVO>> page(ReviewQuery query) {
        IPage<ReviewAdminVO> page = reviewMapper.selectAdminPage(
                new Page<>(query.getCurrent(), query.getSize()), query);
        return Result.ok(PageResult.of(page));
    }

    @PostMapping("/{id}/reply")
    public Result<Void> reply(@PathVariable Long id, @RequestBody ReplyRequest req) {
        Review review = requireReview(id);
        review.setReplyContent(req.getReplyContent());
        review.setReplyTime(LocalDateTime.now());
        review.setReplyAdminId(AuthContext.get());
        reviewService.updateById(review);
        operationLogService.record(3, AuthContext.get(), "回复评价: " + review.getReviewNo());
        return Result.ok();
    }

    @PutMapping("/{id}/audit")
    public Result<Void> audit(@PathVariable Long id, @RequestBody AuditRequest req) {
        Review review = requireReview(id);
        if (req.getAuditStatus() == null || (req.getAuditStatus() != 1 && req.getAuditStatus() != 2)) {
            throw new BusinessException("审核状态不合法");
        }
        review.setAuditStatus(req.getAuditStatus());
        reviewService.updateById(review);
        operationLogService.record(3, AuthContext.get(), "审核评价: " + review.getReviewNo());
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        reviewService.removeById(id);
        operationLogService.record(4, AuthContext.get(), "删除评价: " + id);
        return Result.ok();
    }

    private Review requireReview(Long id) {
        Review review = reviewService.getById(id);
        if (review == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "评价不存在");
        }
        return review;
    }
}
