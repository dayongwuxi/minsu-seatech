package com.seatech.minsu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.seatech.minsu.dto.BookingAdminVO;
import com.seatech.minsu.dto.BookingQuery;
import com.seatech.minsu.dto.BookingUserVO;
import com.seatech.minsu.dto.HotRoomVO;
import com.seatech.minsu.dto.TrendPointVO;
import com.seatech.minsu.entity.Booking;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

public interface BookingMapper extends BaseMapper<Booking> {

    /** 管理端预定分页(联表会员/房间) */
    @Select("""
            <script>
            SELECT b.id, b.order_no AS orderNo, b.member_id AS memberId, b.room_id AS roomId,
                   b.checkin_date AS checkinDate, b.checkout_date AS checkoutDate, b.nights,
                   b.guest_count AS guestCount, b.guest_name AS guestName, b.contact_phone AS contactPhone,
                   b.unit_price AS unitPrice, b.discount_amount AS discountAmount, b.total_amount AS totalAmount,
                   b.pay_status AS payStatus, b.booking_status AS bookingStatus,
                   b.user_remark AS userRemark, b.admin_remark AS adminRemark, b.create_time AS createTime,
                   m.name AS memberName, m.username AS memberUsername, m.phone AS memberPhone,
                   r.room_name AS roomName, r.room_no AS roomNo
            FROM booking b
            LEFT JOIN member m ON b.member_id = m.id
            LEFT JOIN room r ON b.room_id = r.id
            WHERE b.deleted = 0
            <if test="q.orderNo != null and q.orderNo != ''"> AND b.order_no LIKE CONCAT('%', #{q.orderNo}, '%')</if>
            <if test="q.memberName != null and q.memberName != ''">
              AND (m.name LIKE CONCAT('%', #{q.memberName}, '%') OR m.username LIKE CONCAT('%', #{q.memberName}, '%'))
            </if>
            <if test="q.roomName != null and q.roomName != ''"> AND r.room_name LIKE CONCAT('%', #{q.roomName}, '%')</if>
            <if test="q.checkinStart != null"> AND b.checkin_date &gt;= #{q.checkinStart}</if>
            <if test="q.checkinEnd != null"> AND b.checkin_date &lt;= #{q.checkinEnd}</if>
            <if test="q.payStatus != null"> AND b.pay_status = #{q.payStatus}</if>
            <if test="q.bookingStatus != null"> AND b.booking_status = #{q.bookingStatus}</if>
            ORDER BY b.create_time DESC
            </script>
            """)
    IPage<BookingAdminVO> selectAdminPage(Page<BookingAdminVO> page, @Param("q") BookingQuery q);

    /** 会员端我的预约分页, tab: waitPay/paid/finished/cancelled */
    @Select("""
            <script>
            SELECT b.id, b.order_no AS orderNo, b.room_id AS roomId, b.checkin_date AS checkinDate,
                   b.checkout_date AS checkoutDate, b.nights, b.guest_count AS guestCount,
                   b.total_amount AS totalAmount, b.pay_status AS payStatus, b.booking_status AS bookingStatus,
                   b.create_time AS createTime, r.room_name AS roomName, r.cover_image AS roomCover,
                   (SELECT rr.status FROM refund_record rr WHERE rr.booking_id = b.id
                    ORDER BY rr.apply_time DESC LIMIT 1) AS refundStatus
            FROM booking b LEFT JOIN room r ON b.room_id = r.id
            WHERE b.deleted = 0 AND b.member_id = #{memberId}
            <if test='tab == "waitPay"'> AND b.pay_status = 0 AND b.booking_status IN (0, 1)</if>
            <if test='tab == "paid"'> AND b.pay_status = 1 AND b.booking_status IN (0, 1, 2)</if>
            <if test='tab == "finished"'> AND b.booking_status = 3</if>
            <if test='tab == "cancelled"'> AND b.booking_status = 4</if>
            ORDER BY b.create_time DESC
            </script>
            """)
    IPage<BookingUserVO> selectUserPage(Page<BookingUserVO> page,
                                        @Param("memberId") Long memberId,
                                        @Param("tab") String tab);

    @Select("SELECT COUNT(*) FROM booking WHERE deleted = 0 AND create_time >= #{start} AND create_time < #{end}")
    long countCreatedBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /** 预约量趋势, fmt: %Y-%m-%d 或 %Y-%m */
    @Select("""
            SELECT DATE_FORMAT(create_time, #{fmt}) AS date, COUNT(*) AS value
            FROM booking WHERE deleted = 0 AND create_time >= #{start}
            GROUP BY DATE_FORMAT(create_time, #{fmt}) ORDER BY date
            """)
    List<TrendPointVO> selectBookingTrend(@Param("start") LocalDateTime start, @Param("fmt") String fmt);

    @Select("""
            SELECT r.id AS roomId, r.room_name AS roomName, r.cover_image AS coverImage, COUNT(*) AS bookingCount
            FROM booking b JOIN room r ON b.room_id = r.id
            WHERE b.deleted = 0 AND b.booking_status != 4 AND b.create_time >= #{start}
            GROUP BY r.id, r.room_name, r.cover_image
            ORDER BY bookingCount DESC
            LIMIT #{limit}
            """)
    List<HotRoomVO> selectHotRooms(@Param("start") LocalDateTime start, @Param("limit") int limit);

    @Select("""
            SELECT b.id, b.order_no AS orderNo, b.checkin_date AS checkinDate, b.checkout_date AS checkoutDate,
                   b.nights, b.total_amount AS totalAmount, b.pay_status AS payStatus,
                   b.booking_status AS bookingStatus, b.create_time AS createTime,
                   m.name AS memberName, m.username AS memberUsername, r.room_name AS roomName
            FROM booking b
            LEFT JOIN member m ON b.member_id = m.id
            LEFT JOIN room r ON b.room_id = r.id
            WHERE b.deleted = 0 ORDER BY b.create_time DESC LIMIT #{limit}
            """)
    List<BookingAdminVO> selectLatest(@Param("limit") int limit);
}
