package com.seatech.minsu.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 我的收藏项 */
@Data
public class FavoriteVO {
    private Long id;
    private Long roomId;
    private String roomName;
    private String coverImage;
    private BigDecimal price;
    private Integer roomStatus;
    private Integer shelfStatus;
    private LocalDateTime createTime;
}
