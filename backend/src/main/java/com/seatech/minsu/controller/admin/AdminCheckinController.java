package com.seatech.minsu.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.seatech.minsu.common.BusinessException;
import com.seatech.minsu.common.PageResult;
import com.seatech.minsu.common.Result;
import com.seatech.minsu.common.ResultCode;
import com.seatech.minsu.config.AuthContext;
import com.seatech.minsu.dto.CheckinAdminVO;
import com.seatech.minsu.dto.CheckinQuery;
import com.seatech.minsu.entity.Booking;
import com.seatech.minsu.entity.CheckinRecord;
import com.seatech.minsu.mapper.CheckinRecordMapper;
import com.seatech.minsu.service.BookingService;
import com.seatech.minsu.service.CheckinRecordService;
import com.seatech.minsu.service.OperationLogService;
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

/** 入住记录管理 */
@RestController
@RequestMapping("/api/admin/checkins")
@RequiredArgsConstructor
public class AdminCheckinController {

    private final CheckinRecordService checkinRecordService;
    private final CheckinRecordMapper checkinRecordMapper;
    private final BookingService bookingService;
    private final OperationLogService operationLogService;

    @GetMapping
    public Result<PageResult<CheckinAdminVO>> page(CheckinQuery query) {
        IPage<CheckinAdminVO> page = checkinRecordMapper.selectAdminPage(
                new Page<>(query.getCurrent(), query.getSize()), query);
        return Result.ok(PageResult.of(page));
    }

    /** 办理退房：入住记录置已退房，联动订单置已完成 */
    @PostMapping("/{id}/checkout")
    public Result<Void> checkout(@PathVariable Long id) {
        CheckinRecord record = requireRecord(id);
        if (record.getStatus() != null && record.getStatus() == 2) {
            throw new BusinessException("该记录已退房");
        }
        record.setCheckoutTime(LocalDateTime.now());
        record.setStatus(2);
        checkinRecordService.updateById(record);

        Booking booking = bookingService.getById(record.getBookingId());
        if (booking != null) {
            booking.setBookingStatus(3);
            bookingService.updateById(booking);
        }
        operationLogService.record(3, AuthContext.get(), "办理退房: " + record.getCheckinNo());
        return Result.ok();
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody CheckinRecord record) {
        requireRecord(id);
        record.setId(id);
        checkinRecordService.updateById(record);
        operationLogService.record(3, AuthContext.get(), "修改入住记录: " + id);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        checkinRecordService.removeById(id);
        operationLogService.record(4, AuthContext.get(), "删除入住记录: " + id);
        return Result.ok();
    }

    private CheckinRecord requireRecord(Long id) {
        CheckinRecord record = checkinRecordService.getById(id);
        if (record == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "入住记录不存在");
        }
        return record;
    }
}
