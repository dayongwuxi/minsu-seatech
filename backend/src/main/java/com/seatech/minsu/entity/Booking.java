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
 * 预定记录
 * pay_status: 0待支付 1已支付 2已退款
 * booking_status: 0待确认 1已确认 2已入住 3已完成(已退房) 4已取消
 */
@Data
@TableName("booking")
public class Booking {

    @TableId(type = IdType.AUTO)
    private Long id;
    /** 订单编号 DD+yyyyMMdd+4位随机 */
    private String orderNo;
    private Long memberId;
    private Long roomId;
    private LocalDate checkinDate;
    private LocalDate checkoutDate;
    private Integer nights;
    private Integer guestCount;
    private String guestName;
    private String contactPhone;
    /** 下单时房间单价 */
    private BigDecimal unitPrice;
    /** 房费小计=单价×晚数 */
    private BigDecimal roomFee;
    /** 促销优惠额 */
    private BigDecimal promoDiscount;
    /** 会员折扣额 */
    private BigDecimal memberDiscount;
    /** 命中的促销活动id */
    private Long promotionId;
    /** 使用的优惠码 */
    private String promoCode;
    /** 优惠总额(促销+会员折扣) */
    private BigDecimal discountAmount;
    /** 订单总金额(后端重算防篡改) */
    private BigDecimal totalAmount;
    private String currency;
    private Integer payStatus;
    private Integer bookingStatus;
    private String userRemark;
    private String adminRemark;
    @TableLogic
    private Integer deleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
