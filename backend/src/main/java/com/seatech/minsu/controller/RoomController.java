package com.seatech.minsu.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.seatech.minsu.common.BusinessException;
import com.seatech.minsu.common.PageResult;
import com.seatech.minsu.common.Result;
import com.seatech.minsu.common.ResultCode;
import com.seatech.minsu.dto.RoomDetailVO;
import com.seatech.minsu.dto.RoomReviewVO;
import com.seatech.minsu.entity.Review;
import com.seatech.minsu.entity.Room;
import com.seatech.minsu.entity.RoomImage;
import com.seatech.minsu.entity.RoomType;
import com.seatech.minsu.mapper.ReviewMapper;
import com.seatech.minsu.service.ReviewService;
import com.seatech.minsu.service.RoomImageService;
import com.seatech.minsu.service.RoomService;
import com.seatech.minsu.service.RoomTranslationService;
import com.seatech.minsu.service.RoomTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** 用户端房间列表/详情/评价 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;
    private final RoomTypeService roomTypeService;
    private final RoomImageService roomImageService;
    private final ReviewService reviewService;
    private final ReviewMapper reviewMapper;
    private final RoomTranslationService roomTranslationService;

    @GetMapping("/room-types")
    public Result<List<RoomType>> roomTypes() {
        return Result.ok(roomTypeService.lambdaQuery()
                .eq(RoomType::getStatus, 1)
                .orderByAsc(RoomType::getId)
                .list());
    }

    /** 分页多条件查询；传 checkin/checkout 时排除该区间已被占用的房间 */
    @GetMapping("/rooms")
    public Result<PageResult<Room>> rooms(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) Long typeId,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate checkin,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate checkout,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Integer capacity) {
        LambdaQueryWrapper<Room> wrapper = new LambdaQueryWrapper<Room>()
                .eq(Room::getShelfStatus, 1)
                .eq(typeId != null, Room::getRoomTypeId, typeId)
                .ge(minPrice != null, Room::getPrice, minPrice)
                .le(maxPrice != null, Room::getPrice, maxPrice)
                .ge(capacity != null, Room::getMaxGuests, capacity)
                .orderByDesc(Room::getCreateTime);
        if (checkin != null && checkout != null) {
            if (!checkin.isBefore(checkout)) {
                throw new BusinessException("退房日期必须晚于入住日期");
            }
            wrapper.apply("NOT EXISTS (SELECT 1 FROM booking b WHERE b.room_id = room.id"
                            + " AND b.deleted = 0 AND b.booking_status IN (0, 1, 2)"
                            + " AND b.checkin_date < {0} AND b.checkout_date > {1})",
                    checkout, checkin);
        }
        Page<Room> page = roomService.page(new Page<>(current, size), wrapper);
        return Result.ok(PageResult.of(page));
    }

    @GetMapping("/rooms/{id}")
    public Result<RoomDetailVO> roomDetail(@PathVariable Long id) {
        Room room = roomService.getById(id);
        if (room == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "房间不存在");
        }
        RoomDetailVO vo = new RoomDetailVO();
        vo.setRoom(room);
        if (room.getRoomTypeId() != null) {
            RoomType type = roomTypeService.getById(room.getRoomTypeId());
            vo.setTypeName(type == null ? null : type.getTypeName());
        }
        List<RoomImage> images = roomImageService.lambdaQuery()
                .eq(RoomImage::getRoomId, id)
                .orderByAsc(RoomImage::getSort)
                .list();
        vo.setImages(images);
        vo.setI18n(roomTranslationService.list(id));
        vo.setAvgRating(reviewMapper.avgRating(id));
        vo.setReviewCount(reviewService.lambdaQuery()
                .eq(Review::getRoomId, id)
                .eq(Review::getAuditStatus, 1)
                .count());
        return Result.ok(vo);
    }

    @GetMapping("/rooms/{id}/reviews")
    public Result<PageResult<RoomReviewVO>> roomReviews(@PathVariable Long id,
                                                        @RequestParam(defaultValue = "1") long current,
                                                        @RequestParam(defaultValue = "10") long size) {
        IPage<RoomReviewVO> page = reviewMapper.selectRoomReviewPage(new Page<>(current, size), id);
        return Result.ok(PageResult.of(page));
    }
}
