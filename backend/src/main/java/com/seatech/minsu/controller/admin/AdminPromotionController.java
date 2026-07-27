package com.seatech.minsu.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.seatech.minsu.common.BusinessException;
import com.seatech.minsu.common.NoGenerator;
import com.seatech.minsu.common.PageResult;
import com.seatech.minsu.common.Result;
import com.seatech.minsu.common.ResultCode;
import com.seatech.minsu.config.AuthContext;
import com.seatech.minsu.dto.StatusRequest;
import com.seatech.minsu.entity.Promotion;
import com.seatech.minsu.service.OperationLogService;
import com.seatech.minsu.service.PromotionService;
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

/** 促销管理 */
@RestController
@RequestMapping("/api/admin/promotions")
@RequiredArgsConstructor
public class AdminPromotionController {

    private final PromotionService promotionService;
    private final OperationLogService operationLogService;

    @GetMapping
    public Result<PageResult<Promotion>> page(@RequestParam(defaultValue = "1") long current,
                                              @RequestParam(defaultValue = "10") long size,
                                              @RequestParam(required = false) String name,
                                              @RequestParam(required = false) Integer type,
                                              @RequestParam(required = false) Integer status) {
        Page<Promotion> page = promotionService.lambdaQuery()
                .like(StringUtils.hasText(name), Promotion::getName, name)
                .eq(type != null, Promotion::getType, type)
                .eq(status != null, Promotion::getStatus, status)
                .orderByDesc(Promotion::getCreateTime)
                .page(new Page<>(current, size));
        return Result.ok(PageResult.of(page));
    }

    @GetMapping("/{id}")
    public Result<Promotion> detail(@PathVariable Long id) {
        Promotion promotion = promotionService.getById(id);
        if (promotion == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "促销活动不存在");
        }
        return Result.ok(promotion);
    }

    @PostMapping
    public Result<Void> save(@RequestBody Promotion promotion) {
        validate(promotion);
        if (!StringUtils.hasText(promotion.getPromoNo())) {
            promotion.setPromoNo(NoGenerator.gen("PM"));
        }
        if (promotion.getUsedCount() == null) {
            promotion.setUsedCount(0);
        }
        if (promotion.getStatus() == null) {
            promotion.setStatus(1);
        }
        promotionService.save(promotion);
        operationLogService.record(2, AuthContext.get(), "新增促销活动: " + promotion.getName());
        return Result.ok();
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Promotion promotion) {
        validate(promotion);
        promotion.setId(id);
        promotionService.updateById(promotion);
        operationLogService.record(3, AuthContext.get(), "修改促销活动: " + id);
        return Result.ok();
    }

    @PutMapping("/{id}/status")
    public Result<Void> status(@PathVariable Long id, @RequestBody StatusRequest req) {
        Promotion promotion = new Promotion();
        promotion.setId(id);
        promotion.setStatus(req.getStatus());
        promotionService.updateById(promotion);
        operationLogService.record(3, AuthContext.get(), "修改促销活动状态: " + id);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        promotionService.removeById(id);
        operationLogService.record(4, AuthContext.get(), "删除促销活动: " + id);
        return Result.ok();
    }

    private void validate(Promotion p) {
        if (!StringUtils.hasText(p.getName()) || p.getType() == null
                || p.getStartDate() == null || p.getEndDate() == null) {
            throw new BusinessException("活动名称、类型、有效期不能为空");
        }
        if (p.getEndDate().isBefore(p.getStartDate())) {
            throw new BusinessException("结束日期不能早于开始日期");
        }
        switch (p.getType()) {
            case 1 -> requireRate(p);
            case 2 -> {
                if (p.getDiscountAmount() == null || p.getThresholdAmount() == null) {
                    throw new BusinessException("满减促销须填写门槛与立减金额");
                }
            }
            case 3 -> {
                requireRate(p);
                if (p.getMinNights() == null || p.getMinNights() < 1) {
                    throw new BusinessException("长住折扣须填写最少连住晚数");
                }
            }
            case 4 -> {
                requireRate(p);
                if (p.getAdvanceDays() == null || p.getAdvanceDays() < 1) {
                    throw new BusinessException("早鸟折扣须填写提前预订天数");
                }
            }
            default -> throw new BusinessException("促销类型不合法");
        }
    }

    private void requireRate(Promotion p) {
        if (p.getDiscountRate() == null
                || p.getDiscountRate().doubleValue() <= 0 || p.getDiscountRate().doubleValue() >= 1) {
            throw new BusinessException("折扣率须在(0,1)之间, 如0.8=8折");
        }
    }
}
