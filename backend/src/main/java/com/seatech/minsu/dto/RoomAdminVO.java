package com.seatech.minsu.dto;

import com.seatech.minsu.entity.Room;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 管理端房间列表项(含类型名称) */
@Data
@EqualsAndHashCode(callSuper = true)
public class RoomAdminVO extends Room {
    private String typeName;
}
