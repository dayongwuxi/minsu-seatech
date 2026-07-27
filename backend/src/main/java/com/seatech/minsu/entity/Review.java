package com.seatech.minsu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 评价记录 */
@Data
@TableName("review")
public class Review {

    @TableId(type = IdType.AUTO)
    private Long id;
    /** 评价编号 R+yyyyMMdd+4位随机 */
    private String reviewNo;
    private Long memberId;
    private Long roomId;
    /** 一单一评 */
    private Long bookingId;
    /** 0.5~5.0 支持半星 */
    private BigDecimal rating;
    private String content;
    /** 0待审核 1已通过 2未通过 */
    private Integer auditStatus;
    private String replyContent;
    private LocalDateTime replyTime;
    private Long replyAdminId;
    @TableLogic
    private Integer deleted;
    private LocalDateTime createTime;
}
