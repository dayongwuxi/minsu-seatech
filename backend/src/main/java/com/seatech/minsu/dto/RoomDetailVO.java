package com.seatech.minsu.dto;

import com.seatech.minsu.entity.Room;
import com.seatech.minsu.entity.RoomImage;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/** 房间详情：含图片列表与已通过审核评价的平均分/条数 */
@Data
public class RoomDetailVO {
    private Room room;
    private String typeName;
    private List<RoomImage> images;
    /** 房间说明/须知的多语言译文（base 中文在 room.description/bookingNote） */
    private List<RoomI18nVO> i18n;
    private BigDecimal avgRating;
    private Long reviewCount;
}
