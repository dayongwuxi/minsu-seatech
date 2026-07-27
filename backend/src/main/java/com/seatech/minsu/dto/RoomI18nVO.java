package com.seatech.minsu.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 房间某语言的说明/须知译文（对前端） */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoomI18nVO {
    private String lang;
    private String description;
    private String bookingNote;
}
