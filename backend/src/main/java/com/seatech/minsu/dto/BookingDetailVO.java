package com.seatech.minsu.dto;

import com.seatech.minsu.entity.Booking;
import com.seatech.minsu.entity.Payment;
import com.seatech.minsu.entity.RefundRecord;
import com.seatech.minsu.entity.Room;
import lombok.Data;

/** 订单聚合详情：订单+会员+房间+支付+退款 */
@Data
public class BookingDetailVO {
    private Booking booking;
    private MemberVO member;
    private Room room;
    private Payment payment;
    private RefundRecord refund;
}
