package com.seatech.minsu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.seatech.minsu.dto.MyReviewVO;
import com.seatech.minsu.dto.ReviewAdminVO;
import com.seatech.minsu.dto.ReviewQuery;
import com.seatech.minsu.dto.RoomReviewVO;
import com.seatech.minsu.entity.Review;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;

public interface ReviewMapper extends BaseMapper<Review> {

    /** 房间详情页评价列表(仅已通过审核) */
    @Select("""
            SELECT v.id, v.rating, v.content, v.create_time AS createTime,
                   v.reply_content AS replyContent, v.reply_time AS replyTime,
                   m.username AS memberName, m.avatar AS memberAvatar
            FROM review v LEFT JOIN member m ON v.member_id = m.id
            WHERE v.deleted = 0 AND v.audit_status = 1 AND v.room_id = #{roomId}
            ORDER BY v.create_time DESC
            """)
    IPage<RoomReviewVO> selectRoomReviewPage(Page<RoomReviewVO> page, @Param("roomId") Long roomId);

    @Select("SELECT IFNULL(AVG(rating), 0) FROM review WHERE deleted = 0 AND audit_status = 1 AND room_id = #{roomId}")
    BigDecimal avgRating(@Param("roomId") Long roomId);

    /** 会员端我的评价 */
    @Select("""
            SELECT v.id, v.review_no AS reviewNo, v.room_id AS roomId, v.booking_id AS bookingId,
                   v.rating, v.content, v.audit_status AS auditStatus,
                   v.reply_content AS replyContent, v.reply_time AS replyTime, v.create_time AS createTime,
                   r.room_name AS roomName, r.cover_image AS roomCover
            FROM review v LEFT JOIN room r ON v.room_id = r.id
            WHERE v.deleted = 0 AND v.member_id = #{memberId}
            ORDER BY v.create_time DESC
            """)
    IPage<MyReviewVO> selectMyPage(Page<MyReviewVO> page, @Param("memberId") Long memberId);

    /** 管理端评价分页 */
    @Select("""
            <script>
            SELECT v.id, v.review_no AS reviewNo, v.member_id AS memberId, v.room_id AS roomId,
                   v.booking_id AS bookingId, v.rating, v.content, v.audit_status AS auditStatus,
                   v.reply_content AS replyContent, v.reply_time AS replyTime, v.create_time AS createTime,
                   r.room_name AS roomName, m.name AS memberName, m.username AS memberUsername
            FROM review v
            LEFT JOIN room r ON v.room_id = r.id
            LEFT JOIN member m ON v.member_id = m.id
            WHERE v.deleted = 0
            <if test="q.roomName != null and q.roomName != ''"> AND r.room_name LIKE CONCAT('%', #{q.roomName}, '%')</if>
            <if test="q.memberName != null and q.memberName != ''">
              AND (m.name LIKE CONCAT('%', #{q.memberName}, '%') OR m.username LIKE CONCAT('%', #{q.memberName}, '%'))
            </if>
            <if test="q.rating != null"> AND v.rating = #{q.rating}</if>
            <if test="q.auditStatus != null"> AND v.audit_status = #{q.auditStatus}</if>
            ORDER BY v.create_time DESC
            </script>
            """)
    IPage<ReviewAdminVO> selectAdminPage(Page<ReviewAdminVO> page, @Param("q") ReviewQuery q);
}
