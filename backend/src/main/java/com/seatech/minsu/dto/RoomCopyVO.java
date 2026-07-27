package com.seatech.minsu.dto;

import lombok.Data;

/** AI 房型文案生成结果 */
@Data
public class RoomCopyVO {
    /** 房间说明 */
    private String description;
    /** 预订须知 */
    private String bookingNote;
}
