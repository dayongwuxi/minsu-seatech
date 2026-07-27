package com.seatech.minsu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.seatech.minsu.dto.RefundAdminVO;
import com.seatech.minsu.entity.RefundRecord;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface RefundRecordMapper extends BaseMapper<RefundRecord> {

    /** 管理端退款申请分页(联表订单/会员) */
    @Select("""
            <script>
            SELECT rr.id, rr.booking_id AS bookingId, rr.member_id AS memberId,
                   rr.refund_amount AS refundAmount, rr.reason, rr.status, rr.channel,
                   rr.stripe_refund_id AS stripeRefundId, rr.apply_time AS applyTime,
                   rr.handle_time AS handleTime, rr.handler_id AS handlerId,
                   b.order_no AS orderNo, b.booking_status AS bookingStatus, b.pay_status AS payStatus,
                   m.name AS memberName, m.username AS memberUsername
            FROM refund_record rr
            LEFT JOIN booking b ON rr.booking_id = b.id
            LEFT JOIN member m ON rr.member_id = m.id
            <where>
              <if test="orderNo != null and orderNo != ''"> AND b.order_no LIKE CONCAT('%', #{orderNo}, '%')</if>
              <if test="memberName != null and memberName != ''">
                AND (m.name LIKE CONCAT('%', #{memberName}, '%') OR m.username LIKE CONCAT('%', #{memberName}, '%'))
              </if>
              <if test="status != null"> AND rr.status = #{status}</if>
            </where>
            ORDER BY rr.apply_time DESC
            </script>
            """)
    IPage<RefundAdminVO> selectAdminPage(Page<RefundAdminVO> page,
                                         @Param("orderNo") String orderNo,
                                         @Param("memberName") String memberName,
                                         @Param("status") Integer status);
}
