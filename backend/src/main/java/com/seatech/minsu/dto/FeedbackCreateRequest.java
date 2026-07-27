package com.seatech.minsu.dto;

import lombok.Data;

@Data
public class FeedbackCreateRequest {
    /** 1住宿服务 2设施问题 3订单问题 4环境问题 5其他 */
    private Integer type;
    private String title;
    private String content;
    /** 图片URL,逗号分隔 */
    private String images;
}
