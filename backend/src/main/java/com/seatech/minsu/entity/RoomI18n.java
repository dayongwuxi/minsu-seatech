package com.seatech.minsu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 房间说明/预订须知的多语言译文（base 中文在 room 表） */
@Data
@TableName("room_i18n")
public class RoomI18n {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long roomId;
    /** 语言码 en/ja/ko/fr/de/es/pt/ru */
    private String lang;
    private String description;
    private String bookingNote;
    private LocalDateTime updateTime;
}
