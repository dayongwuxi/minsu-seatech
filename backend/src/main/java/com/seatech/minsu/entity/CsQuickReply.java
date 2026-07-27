package com.seatech.minsu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/** 客服快捷回复 */
@Data
@TableName("cs_quick_reply")
public class CsQuickReply {

    @TableId(type = IdType.AUTO)
    private Long id;
    /** 常见问题 */
    private String question;
    /** 快捷回复内容 */
    private String answer;
    private Integer sort;
    @TableLogic
    private Integer deleted;
}
