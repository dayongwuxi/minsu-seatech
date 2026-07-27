package com.seatech.minsu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 周边设施 */
@Data
@TableName("nearby_facility")
public class NearbyFacility {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String facilityNo;
    private String name;
    /** 地铁站/餐饮美食/便利店/停车场/景点... */
    private String type;
    /** 距离(米) */
    private Integer distance;
    private String address;
    private String imageUrl;
    /** 0维护中 1启用 */
    private Integer status;
    @TableLogic
    private Integer deleted;
    private LocalDateTime createTime;
}
