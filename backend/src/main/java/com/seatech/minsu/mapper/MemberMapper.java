package com.seatech.minsu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.seatech.minsu.dto.MemberAdminVO;
import com.seatech.minsu.entity.Member;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface MemberMapper extends BaseMapper<Member> {

    @Select("""
            <script>
            SELECT m.id, m.member_no AS memberNo, m.username, m.phone,
                   m.phone_country AS phoneCountry, m.name, m.email, m.avatar,
                   m.member_type_id AS memberTypeId, m.status, m.register_time AS registerTime,
                   t.type_name AS memberTypeName
            FROM member m LEFT JOIN member_type t ON m.member_type_id = t.id
            WHERE m.deleted = 0
            <if test="name != null and name != ''">
              AND (m.name LIKE CONCAT('%', #{name}, '%') OR m.username LIKE CONCAT('%', #{name}, '%'))
            </if>
            <if test="phone != null and phone != ''"> AND m.phone LIKE CONCAT('%', #{phone}, '%')</if>
            <if test="typeId != null"> AND m.member_type_id = #{typeId}</if>
            <if test="status != null"> AND m.status = #{status}</if>
            ORDER BY m.register_time DESC
            </script>
            """)
    IPage<MemberAdminVO> selectAdminPage(Page<MemberAdminVO> page,
                                         @Param("name") String name,
                                         @Param("phone") String phone,
                                         @Param("typeId") Long typeId,
                                         @Param("status") Integer status);
}
