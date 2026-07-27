package com.seatech.minsu.llm.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/** 路由表中的一项：逻辑角色 → 具体模型 */
@Data
@AllArgsConstructor
public class LlmModelInfo {
    private String role;
    private String model;
}
