package com.seatech.minsu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.seatech.minsu.dto.CsSessionVO;
import com.seatech.minsu.entity.CsSession;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface CsSessionMapper extends BaseMapper<CsSession> {

    /** 管理端会话分页(联表会员昵称) */
    @Select("""
            <script>
            SELECT s.id, s.member_id AS memberId, s.admin_id AS adminId, s.source_channel AS sourceChannel,
                   s.status, s.last_message AS lastMessage, s.last_time AS lastTime,
                   s.unread_admin AS unreadAdmin, s.unread_member AS unreadMember,
                   s.start_time AS startTime, s.end_time AS endTime,
                   m.username AS memberName, m.avatar AS memberAvatar
            FROM cs_session s LEFT JOIN member m ON s.member_id = m.id
            <where>
              <if test="status != null"> AND s.status = #{status}</if>
              <if test="keyword != null and keyword != ''">
                AND (m.username LIKE CONCAT('%', #{keyword}, '%')
                     OR m.name LIKE CONCAT('%', #{keyword}, '%')
                     OR s.last_message LIKE CONCAT('%', #{keyword}, '%'))
              </if>
            </where>
            ORDER BY s.last_time DESC, s.id DESC
            </script>
            """)
    IPage<CsSessionVO> selectAdminPage(Page<CsSessionVO> page,
                                       @Param("status") Integer status,
                                       @Param("keyword") String keyword);
}
