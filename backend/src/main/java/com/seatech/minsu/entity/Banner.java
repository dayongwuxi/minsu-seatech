package com.seatech.minsu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 首页轮播图 */
@Data
@TableName("banner")
public class Banner {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String imageUrl;
    private String linkUrl;
    private Integer sort;
    /** 0停用 1启用 */
    private Integer status;
    @TableLogic
    private Integer deleted;
    private LocalDateTime createTime;
}
