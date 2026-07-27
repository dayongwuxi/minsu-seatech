package com.seatech.minsu.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.seatech.minsu.common.BusinessException;
import com.seatech.minsu.common.NoGenerator;
import com.seatech.minsu.common.PageResult;
import com.seatech.minsu.common.Result;
import com.seatech.minsu.config.AuthContext;
import com.seatech.minsu.dto.FeedbackCreateRequest;
import com.seatech.minsu.entity.Feedback;
import com.seatech.minsu.service.FeedbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 会员端投诉反馈 */
@RestController
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;

    @PostMapping("/api/feedbacks")
    public Result<Void> create(@RequestBody FeedbackCreateRequest req) {
        if (req.getType() == null || !StringUtils.hasText(req.getTitle())
                || !StringUtils.hasText(req.getContent())) {
            throw new BusinessException("反馈类型、标题、内容不能为空");
        }
        Feedback feedback = new Feedback();
        feedback.setFeedbackNo(NoGenerator.gen("FB"));
        feedback.setMemberId(AuthContext.get());
        feedback.setType(req.getType());
        feedback.setTitle(req.getTitle());
        feedback.setContent(req.getContent());
        feedback.setImages(req.getImages());
        feedback.setStatus(0);
        feedbackService.save(feedback);
        return Result.ok();
    }

    @GetMapping("/api/feedbacks")
    public Result<PageResult<Feedback>> myFeedbacks(@RequestParam(defaultValue = "1") long current,
                                                    @RequestParam(defaultValue = "10") long size) {
        Page<Feedback> page = feedbackService.lambdaQuery()
                .eq(Feedback::getMemberId, AuthContext.get())
                .orderByDesc(Feedback::getCreateTime)
                .page(new Page<>(current, size));
        return Result.ok(PageResult.of(page));
    }
}
