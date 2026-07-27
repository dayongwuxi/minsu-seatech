package com.seatech.minsu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 房间收藏 */
@Data
@TableName("favorite")
public class Favorite {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long memberId;
    private Long roomId;
    private LocalDateTime createTime;
}
