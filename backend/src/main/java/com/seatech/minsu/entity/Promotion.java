package com.seatech.minsu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 促销活动
 * type: 1百分比折扣 2满减 3长住折扣 4早鸟折扣
 */
@Data
@TableName("promotion")
public class Promotion {

    @TableId(type = IdType.AUTO)
    private Long id;
    /** 促销编号 PM+yyyyMMdd+4位随机 */
    private String promoNo;
    /** 活动名称(价格明细中展示) */
    private String name;
    /** 1百分比折扣 2满减 3长住折扣 4早鸟折扣 */
    private Integer type;
    /** 折扣率 0.80=8折 (type=1/3/4) */
    private BigDecimal discountRate;
    /** 立减金额 (type=2) */
    private BigDecimal discountAmount;
    /** 满减门槛 (type=2) */
    private BigDecimal thresholdAmount;
    /** 最少连住晚数 (type=3) */
    private Integer minNights;
    /** 提前预订天数 (type=4) */
    private Integer advanceDays;
    /** 限定房间, NULL=全场 */
    private Long roomId;
    /** 优惠码, NULL=自动应用 */
    private String couponCode;
    private LocalDate startDate;
    private LocalDate endDate;
    /** 总使用次数上限, NULL=不限 */
    private Integer usageLimit;
    private Integer usedCount;
    /** 0停用 1启用 */
    private Integer status;
    @TableLogic
    private Integer deleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
