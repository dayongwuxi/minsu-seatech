package com.seatech.minsu.controller;

import com.seatech.minsu.common.BusinessException;
import com.seatech.minsu.common.NoGenerator;
import com.seatech.minsu.common.Result;
import com.seatech.minsu.config.AuthContext;
import com.seatech.minsu.dto.ReviewCreateRequest;
import com.seatech.minsu.entity.Booking;
import com.seatech.minsu.entity.CheckinRecord;
import com.seatech.minsu.entity.Review;
import com.seatech.minsu.service.BookingService;
import com.seatech.minsu.service.CheckinRecordService;
import com.seatech.minsu.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

/** 会员端发布评价 */
@RestController
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;
    private final BookingService bookingService;
    private final CheckinRecordService checkinRecordService;

    @PostMapping("/api/reviews")
    public Result<Void> create(@RequestBody ReviewCreateRequest req) {
        Long memberId = AuthContext.get();
        if (req.getBookingId() == null || req.getRating() == null) {
            throw new BusinessException("订单与评分不能为空");
        }
        if (req.getRating().compareTo(new BigDecimal("0.5")) < 0
                || req.getRating().compareTo(new BigDecimal("5.0")) > 0) {
            throw new BusinessException("评分范围为0.5~5.0");
        }
        Booking booking = bookingService.getById(req.getBookingId());
        if (booking == null || !booking.getMemberId().equals(memberId)) {
            throw new BusinessException("订单不存在或无权评价");
        }
        if (booking.getBookingStatus() != 3) {
            throw new BusinessException("退房后才能评价");
        }
        long existing = reviewService.lambdaQuery()
                .eq(Review::getBookingId, booking.getId())
                .count();
        if (existing > 0) {
            throw new BusinessException("该订单已评价过");
        }
        Review review = new Review();
        review.setReviewNo(NoGenerator.gen("R"));
        review.setMemberId(memberId);
        review.setRoomId(booking.getRoomId());
        review.setBookingId(booking.getId());
        review.setRating(req.getRating());
        review.setContent(req.getContent());
        review.setAuditStatus(0);
        reviewService.save(review);

        checkinRecordService.lambdaUpdate()
                .eq(CheckinRecord::getBookingId, booking.getId())
                .set(CheckinRecord::getIsReviewed, 1)
                .update();
        return Result.ok();
    }
}
