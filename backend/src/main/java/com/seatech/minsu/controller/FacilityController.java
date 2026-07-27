package com.seatech.minsu.controller;

import com.seatech.minsu.common.Result;
import com.seatech.minsu.entity.NearbyFacility;
import com.seatech.minsu.service.NearbyFacilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 用户端周边设施 */
@RestController
@RequiredArgsConstructor
public class FacilityController {

    private final NearbyFacilityService nearbyFacilityService;

    @GetMapping("/api/facilities")
    public Result<List<NearbyFacility>> facilities(@RequestParam(required = false) String type) {
        return Result.ok(nearbyFacilityService.lambdaQuery()
                .eq(NearbyFacility::getStatus, 1)
                .eq(StringUtils.hasText(type), NearbyFacility::getType, type)
                .orderByAsc(NearbyFacility::getDistance)
                .list());
    }
}
