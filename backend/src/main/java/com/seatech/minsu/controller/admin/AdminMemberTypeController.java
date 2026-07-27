package com.seatech.minsu.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.seatech.minsu.common.NoGenerator;
import com.seatech.minsu.common.PageResult;
import com.seatech.minsu.common.Result;
import com.seatech.minsu.config.AuthContext;
import com.seatech.minsu.dto.StatusRequest;
import com.seatech.minsu.entity.MemberType;
import com.seatech.minsu.service.MemberTypeService;
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

/** 会员类型管理 */
@RestController
@RequestMapping("/api/admin/member-types")
@RequiredArgsConstructor
public class AdminMemberTypeController {

    private final MemberTypeService memberTypeService;
    private final OperationLogService operationLogService;

    @GetMapping
    public Result<PageResult<MemberType>> page(@RequestParam(defaultValue = "1") long current,
                                               @RequestParam(defaultValue = "10") long size,
                                               @RequestParam(required = false) String typeName,
                                               @RequestParam(required = false) Integer status) {
        Page<MemberType> page = memberTypeService.lambdaQuery()
                .like(StringUtils.hasText(typeName), MemberType::getTypeName, typeName)
                .eq(status != null, MemberType::getStatus, status)
                .orderByAsc(MemberType::getId)
                .page(new Page<>(current, size));
        return Result.ok(PageResult.of(page));
    }

    @PostMapping
    public Result<Void> save(@RequestBody MemberType memberType) {
        if (!StringUtils.hasText(memberType.getTypeNo())) {
            memberType.setTypeNo(NoGenerator.gen("MT"));
        }
        memberTypeService.save(memberType);
        operationLogService.record(2, AuthContext.get(), "新增会员类型: " + memberType.getTypeName());
        return Result.ok();
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody MemberType memberType) {
        memberType.setId(id);
        memberTypeService.updateById(memberType);
        operationLogService.record(3, AuthContext.get(), "修改会员类型: " + id);
        return Result.ok();
    }

    @PutMapping("/{id}/status")
    public Result<Void> status(@PathVariable Long id, @RequestBody StatusRequest req) {
        MemberType memberType = new MemberType();
        memberType.setId(id);
        memberType.setStatus(req.getStatus());
        memberTypeService.updateById(memberType);
        operationLogService.record(3, AuthContext.get(), "修改会员类型状态: " + id);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        memberTypeService.removeById(id);
        operationLogService.record(4, AuthContext.get(), "删除会员类型: " + id);
        return Result.ok();
    }
}
