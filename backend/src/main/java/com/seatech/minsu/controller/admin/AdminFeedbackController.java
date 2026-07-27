package com.seatech.minsu.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.seatech.minsu.common.BusinessException;
import com.seatech.minsu.common.PageResult;
import com.seatech.minsu.common.Result;
import com.seatech.minsu.common.ResultCode;
import com.seatech.minsu.config.AuthContext;
import com.seatech.minsu.dto.FeedbackAdminVO;
import com.seatech.minsu.dto.ReplyRequest;
import com.seatech.minsu.dto.StatusRequest;
import com.seatech.minsu.entity.Feedback;
import com.seatech.minsu.mapper.FeedbackMapper;
import com.seatech.minsu.service.FeedbackService;
import com.seatech.minsu.service.OperationLogService;
import lombok.RequiredArgsConstructor;
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

/** 投诉反馈管理 */
@RestController
@RequestMapping("/api/admin/feedbacks")
@RequiredArgsConstructor
public class AdminFeedbackController {

    private final FeedbackService feedbackService;
    private final FeedbackMapper feedbackMapper;
    private final OperationLogService operationLogService;

    @GetMapping
    public Result<PageResult<FeedbackAdminVO>> page(@RequestParam(defaultValue = "1") long current,
                                                    @RequestParam(defaultValue = "10") long size,
                                                    @RequestParam(required = false) Integer status,
                                                    @RequestParam(required = false) Integer type,
                                                    @RequestParam(required = false) String keyword) {
        IPage<FeedbackAdminVO> page = feedbackMapper.selectAdminPage(
                new Page<>(current, size), status, type, keyword);
        return Result.ok(PageResult.of(page));
    }

    /** 回复并自动置为已处理 */
    @PostMapping("/{id}/reply")
    public Result<Void> reply(@PathVariable Long id, @RequestBody ReplyRequest req) {
        Feedback feedback = requireFeedback(id);
        feedback.setReplyContent(req.getReplyContent());
        feedback.setReplyTime(LocalDateTime.now());
        feedback.setHandlerId(AuthContext.get());
        feedback.setStatus(2);
        feedbackService.updateById(feedback);
        operationLogService.record(3, AuthContext.get(), "回复投诉反馈: " + feedback.getFeedbackNo());
        return Result.ok();
    }

    @PutMapping("/{id}/status")
    public Result<Void> status(@PathVariable Long id, @RequestBody StatusRequest req) {
        Feedback feedback = requireFeedback(id);
        feedback.setStatus(req.getStatus());
        feedback.setHandlerId(AuthContext.get());
        feedbackService.updateById(feedback);
        operationLogService.record(3, AuthContext.get(), "修改投诉反馈状态: " + feedback.getFeedbackNo());
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        feedbackService.removeById(id);
        operationLogService.record(4, AuthContext.get(), "删除投诉反馈: " + id);
        return Result.ok();
    }

    private Feedback requireFeedback(Long id) {
        Feedback feedback = feedbackService.getById(id);
        if (feedback == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "投诉反馈不存在");
        }
        return feedback;
    }
}
