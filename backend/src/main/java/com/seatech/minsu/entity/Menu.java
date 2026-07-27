package com.seatech.minsu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 后台菜单 */
@Data
@TableName("menu")
public class Menu {

    @TableId(type = IdType.AUTO)
    private Long id;
    /** 父菜单id, 0为顶级 */
    private Long parentId;
    private String menuName;
    private String icon;
    private String path;
    private Integer sort;
    /** 0停用 1启用 */
    private Integer status;
    @TableLogic
    private Integer deleted;
    private LocalDateTime createTime;
}
