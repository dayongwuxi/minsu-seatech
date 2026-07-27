package com.seatech.minsu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.seatech.minsu.entity.Promotion;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

public interface PromotionMapper extends BaseMapper<Promotion> {

    /**
     * 并发安全地占用一个促销名额（下单成功时调用）。
     *
     * @return 影响行数：0 表示名额已被抢完
     */
    @Update("""
            UPDATE promotion SET used_count = used_count + 1
            WHERE id = #{id} AND deleted = 0
              AND (usage_limit IS NULL OR used_count < usage_limit)
            """)
    int consumeUsage(@Param("id") Long id);
}
