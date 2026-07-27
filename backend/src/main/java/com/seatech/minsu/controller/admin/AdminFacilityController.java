package com.seatech.minsu.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.seatech.minsu.common.NoGenerator;
import com.seatech.minsu.common.PageResult;
import com.seatech.minsu.common.Result;
import com.seatech.minsu.config.AuthContext;
import com.seatech.minsu.dto.StatusRequest;
import com.seatech.minsu.entity.NearbyFacility;
import com.seatech.minsu.service.NearbyFacilityService;
import com.seatech.minsu.service.OperationLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 周边设施管理 */
@RestController
@RequestMapping("/api/admin/facilities")
@RequiredArgsConstructor
public class AdminFacilityController {

    private final NearbyFacilityService nearbyFacilityService;
    private final OperationLogService operationLogService;

    @GetMapping
    public Result<PageResult<NearbyFacility>> page(@RequestParam(defaultValue = "1") long current,
                                                   @RequestParam(defaultValue = "10") long size,
                                                   @RequestParam(required = false) String name,
                                                   @RequestParam(required = false) String type,
                                                   @RequestParam(required = false) Integer status) {
        Page<NearbyFacility> page = nearbyFacilityService.lambdaQuery()
                .like(StringUtils.hasText(name), NearbyFacility::getName, name)
                .eq(StringUtils.hasText(type), NearbyFacility::getType, type)
                .eq(status != null, NearbyFacility::getStatus, status)
                .orderByAsc(NearbyFacility::getId)
                .page(new Page<>(current, size));
        return Result.ok(PageResult.of(page));
    }

    @PostMapping
    public Result<Void> save(@RequestBody NearbyFacility facility) {
        if (!StringUtils.hasText(facility.getFacilityNo())) {
            facility.setFacilityNo(NoGenerator.gen("S"));
        }
        nearbyFacilityService.save(facility);
        operationLogService.record(2, AuthContext.get(), "新增周边设施: " + facility.getName());
        return Result.ok();
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody NearbyFacility facility) {
        facility.setId(id);
        nearbyFacilityService.updateById(facility);
        operationLogService.record(3, AuthContext.get(), "修改周边设施: " + id);
        return Result.ok();
    }

    @PutMapping("/{id}/status")
    public Result<Void> status(@PathVariable Long id, @RequestBody StatusRequest req) {
        NearbyFacility facility = new NearbyFacility();
        facility.setId(id);
        facility.setStatus(req.getStatus());
        nearbyFacilityService.updateById(facility);
        operationLogService.record(3, AuthContext.get(), "修改周边设施状态: " + id);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        nearbyFacilityService.removeById(id);
        operationLogService.record(4, AuthContext.get(), "删除周边设施: " + id);
        return Result.ok();
    }
}
