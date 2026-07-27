package com.seatech.minsu.dto;

import lombok.Data;

/** i18n 内容请求：生成时是中文源，单语言保存时是该语言译文 */
@Data
public class RoomI18nContentRequest {
    private String description;
    private String bookingNote;
}
