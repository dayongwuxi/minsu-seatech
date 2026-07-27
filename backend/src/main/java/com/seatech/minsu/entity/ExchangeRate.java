package com.seatech.minsu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 展示汇率(基准 CNY): 1 CNY = rate 单位目标货币。结算一律 CNY */
@Data
@TableName("exchange_rate")
public class ExchangeRate {

    @TableId(type = IdType.AUTO)
    private Long id;
    /** USD/EUR/GBP/JPY/CNY */
    private String currencyCode;
    private String currencyName;
    private String symbol;
    /** 1 CNY 兑换量 */
    private BigDecimal rate;
    /** 1随API自动刷新 0管理员手工锁定 */
    private Integer autoUpdate;
    /** 1 API 2 手工 */
    private Integer source;
    /** 0停用 1启用 */
    private Integer status;
    private LocalDateTime updateTime;
}
