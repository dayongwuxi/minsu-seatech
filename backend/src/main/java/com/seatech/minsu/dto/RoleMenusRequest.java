package com.seatech.minsu.dto;

import lombok.Data;

import java.util.List;

@Data
public class RoleMenusRequest {
    private List<Long> menuIds;
}
