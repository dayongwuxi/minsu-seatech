package com.seatech.minsu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 投诉反馈 */
@Data
@TableName("feedback")
public class Feedback {

    @TableId(type = IdType.AUTO)
    private Long id;
    /** 反馈编号 FB+yyyyMMdd+4位随机 */
    private String feedbackNo;
    private Long memberId;
    /** 1住宿服务 2设施问题 3订单问题 4环境问题 5其他 */
    private Integer type;
    private String title;
    private String content;
    /** 图片URL,逗号分隔 */
    private String images;
    /** 0待处理 1处理中 2已处理 */
    private Integer status;
    private String replyContent;
    private LocalDateTime replyTime;
    private Long handlerId;
    @TableLogic
    private Integer deleted;
    private LocalDateTime createTime;
}
