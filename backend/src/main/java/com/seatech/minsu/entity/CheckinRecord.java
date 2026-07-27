package com.seatech.minsu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 入住记录 */
@Data
@TableName("checkin_record")
public class CheckinRecord {

    @TableId(type = IdType.AUTO)
    private Long id;
    /** 入住编号 CR+yyyyMMdd+4位随机 */
    private String checkinNo;
    private Long bookingId;
    private Long memberId;
    private Long roomId;
    private String guestName;
    /** 实际入住时间 */
    private LocalDateTime checkinTime;
    /** 实际退房时间 */
    private LocalDateTime checkoutTime;
    /** 0待入住 1已入住 2已退房 */
    private Integer status;
    /** 是否已评价 0否 1是 */
    private Integer isReviewed;
    @TableLogic
    private Integer deleted;
    private LocalDateTime createTime;
}
