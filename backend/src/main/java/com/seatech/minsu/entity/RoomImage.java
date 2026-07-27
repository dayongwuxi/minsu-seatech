package com.seatech.minsu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/** 房间图片 */
@Data
@TableName("room_image")
public class RoomImage {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long roomId;
    private String imageUrl;
    private Integer sort;
}
