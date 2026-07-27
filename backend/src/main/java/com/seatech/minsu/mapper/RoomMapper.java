package com.seatech.minsu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.seatech.minsu.entity.Room;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface RoomMapper extends BaseMapper<Room> {

    /** 房间行级锁（须在事务内调用）：串行化同房源并发下单，防占用校验与落单间的双预订窗口 */
    @Select("SELECT id FROM room WHERE id = #{id} FOR UPDATE")
    Long lockById(@Param("id") Long id);
}
