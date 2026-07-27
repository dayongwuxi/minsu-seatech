package com.seatech.minsu.dto;

import lombok.Data;

/** 热门房间 */
@Data
public class HotRoomVO {
    private Long roomId;
    private String roomName;
    private String coverImage;
    private Long bookingCount;
}
