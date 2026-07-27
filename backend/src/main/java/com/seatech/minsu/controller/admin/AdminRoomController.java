package com.seatech.minsu.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.seatech.minsu.common.BusinessException;
import com.seatech.minsu.common.ImageUrls;
import com.seatech.minsu.common.NoGenerator;
import com.seatech.minsu.common.PageResult;
import com.seatech.minsu.common.Result;
import com.seatech.minsu.common.ResultCode;
import com.seatech.minsu.config.AuthContext;
import com.seatech.minsu.dto.RoomAdminVO;
import com.seatech.minsu.dto.RoomDetailVO;
import com.seatech.minsu.dto.RoomI18nContentRequest;
import com.seatech.minsu.dto.RoomI18nVO;
import com.seatech.minsu.dto.RoomSaveRequest;
import com.seatech.minsu.dto.StatusRequest;
import com.seatech.minsu.service.RoomTranslationService;
import com.seatech.minsu.entity.Room;
import com.seatech.minsu.entity.RoomImage;
import com.seatech.minsu.entity.RoomType;
import com.seatech.minsu.mapper.ReviewMapper;
import com.seatech.minsu.service.OperationLogService;
import com.seatech.minsu.service.RoomImageService;
import com.seatech.minsu.service.RoomService;
import com.seatech.minsu.service.RoomTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** 房间信息管理 */
@RestController
@RequestMapping("/api/admin/rooms")
@RequiredArgsConstructor
public class AdminRoomController {

    private final RoomService roomService;
    private final RoomTypeService roomTypeService;
    private final RoomImageService roomImageService;
    private final ReviewMapper reviewMapper;
    private final OperationLogService operationLogService;
    private final RoomTranslationService roomTranslationService;

    @GetMapping
    public Result<PageResult<RoomAdminVO>> page(@RequestParam(defaultValue = "1") long current,
                                                @RequestParam(defaultValue = "10") long size,
                                                @RequestParam(required = false) String name,
                                                @RequestParam(required = false) String roomNo,
                                                @RequestParam(required = false) Long typeId,
                                                @RequestParam(required = false) Integer roomStatus,
                                                @RequestParam(required = false) BigDecimal minPrice,
                                                @RequestParam(required = false) BigDecimal maxPrice) {
        Page<Room> page = roomService.lambdaQuery()
                .like(StringUtils.hasText(name), Room::getRoomName, name)
                .like(StringUtils.hasText(roomNo), Room::getRoomNo, roomNo)
                .eq(typeId != null, Room::getRoomTypeId, typeId)
                .eq(roomStatus != null, Room::getRoomStatus, roomStatus)
                .ge(minPrice != null, Room::getPrice, minPrice)
                .le(maxPrice != null, Room::getPrice, maxPrice)
                .orderByDesc(Room::getCreateTime)
                .page(new Page<>(current, size));

        Map<Long, String> typeNames = roomTypeService.list().stream()
                .collect(Collectors.toMap(RoomType::getId, RoomType::getTypeName, (a, b) -> a));
        Page<RoomAdminVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(page.getRecords().stream().map(room -> {
            RoomAdminVO vo = new RoomAdminVO();
            BeanUtils.copyProperties(room, vo);
            vo.setTypeName(typeNames.get(room.getRoomTypeId()));
            return vo;
        }).collect(Collectors.toList()));
        return Result.ok(PageResult.of(voPage));
    }

    @GetMapping("/{id}")
    public Result<RoomDetailVO> detail(@PathVariable Long id) {
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
        vo.setImages(roomImageService.lambdaQuery()
                .eq(RoomImage::getRoomId, id)
                .orderByAsc(RoomImage::getSort)
                .list());
        vo.setI18n(roomTranslationService.list(id));
        vo.setAvgRating(reviewMapper.avgRating(id));
        return Result.ok(vo);
    }

    /** 一键生成多语言：把当前中文说明/须知翻译为全部目标语种并落库 */
    @PostMapping("/{id}/i18n/generate")
    public Result<List<RoomI18nVO>> generateI18n(@PathVariable Long id,
                                                 @RequestBody RoomI18nContentRequest req) {
        if (roomService.getById(id) == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "房间不存在");
        }
        List<RoomI18nVO> list = roomTranslationService.generateAll(id, req.getDescription(), req.getBookingNote());
        operationLogService.record(3, AuthContext.get(), "生成房间多语言文案: " + id);
        return Result.ok(list);
    }

    /** 保存/覆盖单个语言版本 */
    @PutMapping("/{id}/i18n/{lang}")
    public Result<RoomI18nVO> saveI18n(@PathVariable Long id, @PathVariable String lang,
                                       @RequestBody RoomI18nContentRequest req) {
        if (roomService.getById(id) == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "房间不存在");
        }
        RoomI18nVO vo = roomTranslationService.saveOne(id, lang, req.getDescription(), req.getBookingNote());
        operationLogService.record(3, AuthContext.get(), "编辑房间 " + lang + " 文案: " + id);
        return Result.ok(vo);
    }

    @PostMapping
    public Result<Void> save(@RequestBody RoomSaveRequest req) {
        Room room = buildRoom(req);
        // 兜底：只接受本站 /files/ 地址，封面取第一张有效图，杜绝 blob: 等临时地址入库
        List<String> images = ImageUrls.sanitize(req.getImages());
        room.setCoverImage(images.isEmpty() ? null : images.get(0));
        if (!StringUtils.hasText(room.getRoomNo())) {
            room.setRoomNo(NoGenerator.gen("RM"));
        }
        roomService.save(room);
        rebuildImages(room.getId(), images);
        operationLogService.record(2, AuthContext.get(), "新增房间: " + room.getRoomName());
        return Result.ok();
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody RoomSaveRequest req) {
        Room existing = roomService.getById(id);
        if (existing == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "房间不存在");
        }
        Room room = buildRoom(req);
        room.setId(id);
        List<String> images = ImageUrls.sanitize(req.getImages());
        room.setCoverImage(images.isEmpty() ? null : images.get(0));
        if (!StringUtils.hasText(room.getRoomNo())) {
            room.setRoomNo(existing.getRoomNo());
        }
        roomService.updateById(room);
        rebuildImages(id, images);
        operationLogService.record(3, AuthContext.get(), "修改房间: " + id);
        return Result.ok();
    }

    @PutMapping("/{id}/shelf-status")
    public Result<Void> shelfStatus(@PathVariable Long id, @RequestBody StatusRequest req) {
        Room room = new Room();
        room.setId(id);
        room.setShelfStatus(req.getStatus());
        roomService.updateById(room);
        operationLogService.record(3, AuthContext.get(), "修改房间上下架状态: " + id);
        return Result.ok();
    }

    @PutMapping("/{id}/room-status")
    public Result<Void> roomStatus(@PathVariable Long id, @RequestBody StatusRequest req) {
        Room room = new Room();
        room.setId(id);
        room.setRoomStatus(req.getStatus());
        roomService.updateById(room);
        operationLogService.record(3, AuthContext.get(), "修改房间状态: " + id);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        roomService.removeById(id);
        roomImageService.lambdaUpdate().eq(RoomImage::getRoomId, id).remove();
        roomTranslationService.deleteByRoom(id);
        operationLogService.record(4, AuthContext.get(), "删除房间: " + id);
        return Result.ok();
    }

    private Room buildRoom(RoomSaveRequest req) {
        Room room = new Room();
        room.setRoomNo(req.getRoomNo());
        room.setRoomName(req.getRoomName());
        room.setRoomTypeId(req.getRoomTypeId());
        room.setPrice(req.getPrice());
        room.setOriginPrice(req.getOriginPrice());
        room.setMaxGuests(req.getMaxGuests());
        room.setArea(req.getArea());
        room.setBedInfo(req.getBedInfo());
        room.setFacilities(req.getFacilities());
        room.setCoverImage(req.getCoverImage());
        room.setDescription(req.getDescription());
        room.setBookingNote(req.getBookingNote());
        room.setIsRecommend(req.getIsRecommend());
        room.setRoomStatus(req.getRoomStatus());
        room.setShelfStatus(req.getShelfStatus());
        return room;
    }

    /** 保存时重建 room_image */
    private void rebuildImages(Long roomId, List<String> images) {
        roomImageService.lambdaUpdate().eq(RoomImage::getRoomId, roomId).remove();
        if (images == null || images.isEmpty()) {
            return;
        }
        List<RoomImage> list = new ArrayList<>();
        for (int i = 0; i < images.size(); i++) {
            RoomImage image = new RoomImage();
            image.setRoomId(roomId);
            image.setImageUrl(images.get(i));
            image.setSort(i);
            list.add(image);
        }
        roomImageService.saveBatch(list);
    }
}
