package com.seatech.minsu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.seatech.minsu.dto.FavoriteVO;
import com.seatech.minsu.entity.Favorite;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface FavoriteMapper extends BaseMapper<Favorite> {

    /** 会员端我的收藏(联表房间) */
    @Select("""
            SELECT f.id, f.room_id AS roomId, f.create_time AS createTime,
                   r.room_name AS roomName, r.cover_image AS coverImage, r.price,
                   r.room_status AS roomStatus, r.shelf_status AS shelfStatus
            FROM favorite f LEFT JOIN room r ON f.room_id = r.id
            WHERE f.member_id = #{memberId}
            ORDER BY f.create_time DESC
            """)
    IPage<FavoriteVO> selectMyPage(Page<FavoriteVO> page, @Param("memberId") Long memberId);
}
