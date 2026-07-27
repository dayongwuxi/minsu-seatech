package com.seatech.minsu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 会员类型 */
@Data
@TableName("member_type")
public class MemberType {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String typeNo;
    private String typeName;
    /** 折扣率 0.95=9.5折 */
    private BigDecimal discount;
    private String benefits;
    /** 0停用 1启用 */
    private Integer status;
    @TableLogic
    private Integer deleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
