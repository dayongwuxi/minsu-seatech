package com.seatech.minsu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.seatech.minsu.dto.CheckinAdminVO;
import com.seatech.minsu.dto.CheckinQuery;
import com.seatech.minsu.dto.CheckinUserVO;
import com.seatech.minsu.entity.CheckinRecord;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;

public interface CheckinRecordMapper extends BaseMapper<CheckinRecord> {

    /** 会员端我的入住记录 */
    @Select("""
            SELECT c.id, c.checkin_no AS checkinNo, c.booking_id AS bookingId, c.room_id AS roomId,
                   c.guest_name AS guestName, c.checkin_time AS checkinTime, c.checkout_time AS checkoutTime,
                   c.status, c.is_reviewed AS isReviewed, c.create_time AS createTime,
                   r.room_name AS roomName, r.cover_image AS roomCover,
                   b.order_no AS orderNo, b.checkin_date AS checkinDate, b.checkout_date AS checkoutDate
            FROM checkin_record c
            LEFT JOIN room r ON c.room_id = r.id
            LEFT JOIN booking b ON c.booking_id = b.id
            WHERE c.deleted = 0 AND c.member_id = #{memberId}
            ORDER BY c.create_time DESC
            """)
    IPage<CheckinUserVO> selectUserPage(Page<CheckinUserVO> page, @Param("memberId") Long memberId);

    /** 管理端入住记录分页 */
    @Select("""
            <script>
            SELECT c.id, c.checkin_no AS checkinNo, c.booking_id AS bookingId, c.member_id AS memberId,
                   c.room_id AS roomId, c.guest_name AS guestName, c.checkin_time AS checkinTime,
                   c.checkout_time AS checkoutTime, c.status, c.is_reviewed AS isReviewed,
                   c.create_time AS createTime,
                   r.room_name AS roomName, r.room_no AS roomNo, b.order_no AS orderNo,
                   m.name AS memberName, m.username AS memberUsername
            FROM checkin_record c
            LEFT JOIN room r ON c.room_id = r.id
            LEFT JOIN booking b ON c.booking_id = b.id
            LEFT JOIN member m ON c.member_id = m.id
            WHERE c.deleted = 0
            <if test="q.guestName != null and q.guestName != ''"> AND c.guest_name LIKE CONCAT('%', #{q.guestName}, '%')</if>
            <if test="q.roomName != null and q.roomName != ''"> AND r.room_name LIKE CONCAT('%', #{q.roomName}, '%')</if>
            <if test="q.dateStart != null"> AND DATE(c.create_time) &gt;= #{q.dateStart}</if>
            <if test="q.dateEnd != null"> AND DATE(c.create_time) &lt;= #{q.dateEnd}</if>
            <if test="q.status != null"> AND c.status = #{q.status}</if>
            ORDER BY c.create_time DESC
            </script>
            """)
    IPage<CheckinAdminVO> selectAdminPage(Page<CheckinAdminVO> page, @Param("q") CheckinQuery q);

    @Select("SELECT COUNT(*) FROM checkin_record WHERE deleted = 0 AND checkin_time >= #{start} AND checkin_time < #{end}")
    long countCheckinBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
