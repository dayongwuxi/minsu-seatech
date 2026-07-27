package com.seatech.minsu.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/** 管理端房间新增/修改请求 */
@Data
public class RoomSaveRequest {
    private String roomNo;
    private String roomName;
    private Long roomTypeId;
    private BigDecimal price;
    private BigDecimal originPrice;
    private Integer maxGuests;
    private BigDecimal area;
    private String bedInfo;
    private String facilities;
    private String coverImage;
    private String description;
    private String bookingNote;
    private Integer isRecommend;
    private Integer roomStatus;
    private Integer shelfStatus;
    /** 房间图片URL列表，保存时重建 room_image */
    private List<String> images;
}
