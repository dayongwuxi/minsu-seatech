package com.seatech.minsu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 房间信息 */
@Data
@TableName("room")
public class Room {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String roomNo;
    private String roomName;
    private Long roomTypeId;
    /** 现价/晚 */
    private BigDecimal price;
    /** 划线原价/晚 */
    private BigDecimal originPrice;
    private Integer maxGuests;
    private BigDecimal area;
    private String bedInfo;
    private String facilities;
    private String coverImage;
    private String description;
    private String bookingNote;
    /** 首页推荐 0否 1是 */
    private Integer isRecommend;
    /** 0可预订 1维修中 2已满房 */
    private Integer roomStatus;
    /** 0已下架 1已上架 */
    private Integer shelfStatus;
    @TableLogic
    private Integer deleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
