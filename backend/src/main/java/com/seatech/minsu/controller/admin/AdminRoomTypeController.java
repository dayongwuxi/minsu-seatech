package com.seatech.minsu.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.seatech.minsu.common.NoGenerator;
import com.seatech.minsu.common.PageResult;
import com.seatech.minsu.common.Result;
import com.seatech.minsu.config.AuthContext;
import com.seatech.minsu.dto.StatusRequest;
import com.seatech.minsu.entity.RoomType;
import com.seatech.minsu.service.OperationLogService;
import com.seatech.minsu.service.RoomTypeService;
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

/** 房间类型管理 */
@RestController
@RequestMapping("/api/admin/room-types")
@RequiredArgsConstructor
public class AdminRoomTypeController {

    private final RoomTypeService roomTypeService;
    private final OperationLogService operationLogService;

    @GetMapping
    public Result<PageResult<RoomType>> page(@RequestParam(defaultValue = "1") long current,
                                             @RequestParam(defaultValue = "10") long size,
                                             @RequestParam(required = false) String typeName,
                                             @RequestParam(required = false) Integer status) {
        Page<RoomType> page = roomTypeService.lambdaQuery()
                .like(StringUtils.hasText(typeName), RoomType::getTypeName, typeName)
                .eq(status != null, RoomType::getStatus, status)
                .orderByAsc(RoomType::getId)
                .page(new Page<>(current, size));
        return Result.ok(PageResult.of(page));
    }

    @PostMapping
    public Result<Void> save(@RequestBody RoomType roomType) {
        if (!StringUtils.hasText(roomType.getTypeNo())) {
            roomType.setTypeNo(NoGenerator.gen("RT"));
        }
        roomTypeService.save(roomType);
        operationLogService.record(2, AuthContext.get(), "新增房间类型: " + roomType.getTypeName());
        return Result.ok();
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody RoomType roomType) {
        roomType.setId(id);
        roomTypeService.updateById(roomType);
        operationLogService.record(3, AuthContext.get(), "修改房间类型: " + id);
        return Result.ok();
    }

    @PutMapping("/{id}/status")
    public Result<Void> status(@PathVariable Long id, @RequestBody StatusRequest req) {
        RoomType roomType = new RoomType();
        roomType.setId(id);
        roomType.setStatus(req.getStatus());
        roomTypeService.updateById(roomType);
        operationLogService.record(3, AuthContext.get(), "修改房间类型状态: " + id);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        roomTypeService.removeById(id);
        operationLogService.record(4, AuthContext.get(), "删除房间类型: " + id);
        return Result.ok();
    }
}
