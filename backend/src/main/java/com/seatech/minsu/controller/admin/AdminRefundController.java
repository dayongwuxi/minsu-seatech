package com.seatech.minsu.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.seatech.minsu.common.PageResult;
import com.seatech.minsu.common.Result;
import com.seatech.minsu.config.AuthContext;
import com.seatech.minsu.dto.RefundAdminVO;
import com.seatech.minsu.dto.RefundApplyRequest;
import com.seatech.minsu.mapper.RefundRecordMapper;
import com.seatech.minsu.service.OperationLogService;
import com.seatech.minsu.service.RefundService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 退款管理（申请集中处理） */
@RestController
@RequestMapping("/api/admin/refunds")
@RequiredArgsConstructor
public class AdminRefundController {

    private final RefundRecordMapper refundRecordMapper;
    private final RefundService refundService;
    private final OperationLogService operationLogService;

    @GetMapping
    public Result<PageResult<RefundAdminVO>> page(@RequestParam(defaultValue = "1") long current,
                                                  @RequestParam(defaultValue = "10") long size,
                                                  @RequestParam(required = false) String orderNo,
                                                  @RequestParam(required = false) String memberName,
                                                  @RequestParam(required = false) Integer status) {
        IPage<RefundAdminVO> page = refundRecordMapper.selectAdminPage(
                new Page<>(current, size), orderNo, memberName, status);
        return Result.ok(PageResult.of(page));
    }

    /** 同意退款：Stripe 单进入退款中等待 webhook，Mock 单即时退款 */
    @PutMapping("/{id}/approve")
    public Result<Void> approve(@PathVariable Long id) {
        Long adminId = AuthContext.get();
        refundService.approve(id, adminId);
        operationLogService.record(3, adminId, "同意退款申请: " + id);
        return Result.ok();
    }

    /** 确认线下退款完成：MANUAL_OFFLINE 渠道(LinkTrust)在渠道后台手工退款后置已退款 */
    @PutMapping("/{id}/complete")
    public Result<Void> complete(@PathVariable Long id) {
        Long adminId = AuthContext.get();
        refundService.complete(id, adminId);
        operationLogService.record(3, adminId, "确认线下退款完成: " + id);
        return Result.ok();
    }

    @PutMapping("/{id}/reject")
    public Result<Void> reject(@PathVariable Long id,
                               @RequestBody(required = false) RefundApplyRequest req) {
        Long adminId = AuthContext.get();
        refundService.reject(id, adminId, req == null ? null : req.getReason());
        operationLogService.record(3, adminId, "拒绝退款申请: " + id);
        return Result.ok();
    }
}
