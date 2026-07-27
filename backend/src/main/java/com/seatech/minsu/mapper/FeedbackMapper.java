package com.seatech.minsu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.seatech.minsu.dto.FeedbackAdminVO;
import com.seatech.minsu.entity.Feedback;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface FeedbackMapper extends BaseMapper<Feedback> {

    /** 管理端投诉反馈分页(联表会员) */
    @Select("""
            <script>
            SELECT f.id, f.feedback_no AS feedbackNo, f.member_id AS memberId, f.type, f.title,
                   f.content, f.images, f.status, f.reply_content AS replyContent,
                   f.reply_time AS replyTime, f.create_time AS createTime,
                   m.name AS memberName, m.username AS memberUsername, m.phone AS memberPhone
            FROM feedback f LEFT JOIN member m ON f.member_id = m.id
            WHERE f.deleted = 0
            <if test="status != null"> AND f.status = #{status}</if>
            <if test="type != null"> AND f.type = #{type}</if>
            <if test="keyword != null and keyword != ''">
              AND (f.title LIKE CONCAT('%', #{keyword}, '%')
                   OR f.feedback_no LIKE CONCAT('%', #{keyword}, '%')
                   OR m.username LIKE CONCAT('%', #{keyword}, '%'))
            </if>
            ORDER BY f.create_time DESC
            </script>
            """)
    IPage<FeedbackAdminVO> selectAdminPage(Page<FeedbackAdminVO> page,
                                           @Param("status") Integer status,
                                           @Param("type") Integer type,
                                           @Param("keyword") String keyword);
}
