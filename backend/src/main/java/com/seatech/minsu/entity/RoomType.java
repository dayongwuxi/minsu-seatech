package com.seatech.minsu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 房间类型 */
@Data
@TableName("room_type")
public class RoomType {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String typeNo;
    private String typeName;
    private String description;
    /** 0停用 1启用 */
    private Integer status;
    @TableLogic
    private Integer deleted;
    private LocalDateTime createTime;
}
