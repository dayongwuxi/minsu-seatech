package com.seatech.minsu.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.seatech.minsu.common.BusinessException;
import com.seatech.minsu.config.StripeConfig;
import com.seatech.minsu.dto.QuoteVO;
import com.seatech.minsu.entity.Member;
import com.seatech.minsu.entity.MemberType;
import com.seatech.minsu.entity.Promotion;
import com.seatech.minsu.entity.Room;
import com.seatech.minsu.mapper.MemberMapper;
import com.seatech.minsu.mapper.MemberTypeMapper;
import com.seatech.minsu.mapper.PromotionMapper;
import com.seatech.minsu.mapper.RoomMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * 价格计算唯一入口（报价与下单共用同一逻辑，防前端篡改）。
 * 叠加顺序：房费小计 → 促销优惠(自动匹配+优惠码, 取优惠额最大一条)
 *          → 会员折扣(基于扣除促销后的余额) → 总额(下限 0.01)
 */
@Service
@RequiredArgsConstructor
public class PriceService {

    private static final BigDecimal MIN_TOTAL_DECIMAL = new BigDecimal("0.01");

    private final RoomMapper roomMapper;
    private final MemberMapper memberMapper;
    private final MemberTypeMapper memberTypeMapper;
    private final PromotionMapper promotionMapper;
    private final StripeConfig stripeConfig;

    public QuoteVO quote(Long memberId, Long roomId, LocalDate checkin, LocalDate checkout, String promoCode) {
        if (roomId == null || checkin == null || checkout == null) {
            throw new BusinessException("房间与入住/退房日期不能为空");
        }
        if (!checkin.isBefore(checkout)) {
            throw new BusinessException("退房日期必须晚于入住日期");
        }
        LocalDate today = LocalDate.now();
        if (checkin.isBefore(today)) {
            throw new BusinessException("入住日期不能早于今天");
        }
        Room room = roomMapper.selectById(roomId);
        if (room == null || room.getShelfStatus() == null || room.getShelfStatus() != 1) {
            throw new BusinessException("房间不存在或已下架");
        }

        int nights = (int) ChronoUnit.DAYS.between(checkin, checkout);
        BigDecimal unitPrice = room.getPrice();
        BigDecimal roomFee = unitPrice.multiply(BigDecimal.valueOf(nights));

        // 促销候选：自动促销(条件不满足的静默跳过) + 优惠码促销(不满足时抛具体原因)
        List<Promotion> candidates = new ArrayList<>();
        List<Promotion> autos = promotionMapper.selectList(new LambdaQueryWrapper<Promotion>()
                .eq(Promotion::getStatus, 1)
                .isNull(Promotion::getCouponCode)
                .le(Promotion::getStartDate, today)
                .ge(Promotion::getEndDate, today)
                .and(w -> w.isNull(Promotion::getRoomId).or().eq(Promotion::getRoomId, roomId)));
        for (Promotion p : autos) {
            if (isEligible(p, roomFee, nights, checkin, today)) {
                candidates.add(p);
            }
        }
        if (StringUtils.hasText(promoCode)) {
            candidates.add(validateCoupon(promoCode.trim(), roomId, roomFee, nights, checkin, today));
        }

        // 同时命中多条取优惠额最大的一条
        Promotion best = null;
        BigDecimal bestDiscount = BigDecimal.ZERO;
        for (Promotion p : candidates) {
            BigDecimal d = discountOf(p, roomFee);
            if (d.compareTo(bestDiscount) > 0) {
                best = p;
                bestDiscount = d;
            }
        }
        BigDecimal promoDiscount = bestDiscount.min(roomFee);

        // 会员折扣基于(房费 - 促销优惠)
        BigDecimal memberRate = BigDecimal.ONE;
        Member member = memberId == null ? null : memberMapper.selectById(memberId);
        if (member != null && member.getMemberTypeId() != null) {
            MemberType type = memberTypeMapper.selectById(member.getMemberTypeId());
            if (type != null && type.getDiscount() != null) {
                memberRate = type.getDiscount();
            }
        }
        BigDecimal memberDiscount = roomFee.subtract(promoDiscount)
                .multiply(BigDecimal.ONE.subtract(memberRate))
                .setScale(2, RoundingMode.HALF_UP);

        // 零小数货币(jpy)下限 1，否则渠道整数取整后会得到 amount=0
        BigDecimal minTotal = stripeConfig.isZeroDecimalCurrency() ? BigDecimal.ONE : MIN_TOTAL_DECIMAL;
        BigDecimal total = roomFee.subtract(promoDiscount).subtract(memberDiscount);
        if (total.compareTo(minTotal) < 0) {
            total = minTotal;
        }

        QuoteVO vo = new QuoteVO();
        vo.setNights(nights);
        vo.setUnitPrice(unitPrice);
        vo.setRoomFee(roomFee);
        vo.setPromoDiscount(promoDiscount);
        if (best != null) {
            vo.setPromoName(best.getName());
            vo.setPromotionId(best.getId());
            vo.setPromoCode(best.getCouponCode());
        }
        vo.setMemberDiscount(memberDiscount);
        vo.setTotalAmount(total);
        vo.setCurrency(stripeConfig.getCurrency());
        return vo;
    }

    /** 自动促销资格判定（不满足静默跳过） */
    private boolean isEligible(Promotion p, BigDecimal roomFee, int nights, LocalDate checkin, LocalDate today) {
        if (p.getType() == null) {
            return false;
        }
        if (p.getUsageLimit() != null && p.getUsedCount() != null && p.getUsedCount() >= p.getUsageLimit()) {
            return false;
        }
        int type = p.getType();
        return switch (type) {
            case 1 -> p.getDiscountRate() != null;
            case 2 -> p.getDiscountAmount() != null && p.getThresholdAmount() != null
                    && roomFee.compareTo(p.getThresholdAmount()) >= 0;
            case 3 -> p.getDiscountRate() != null && p.getMinNights() != null && nights >= p.getMinNights();
            case 4 -> p.getDiscountRate() != null && p.getAdvanceDays() != null
                    && ChronoUnit.DAYS.between(today, checkin) >= p.getAdvanceDays();
            default -> false;
        };
    }

    /** 优惠码校验（不满足抛具体原因） */
    private Promotion validateCoupon(String code, Long roomId, BigDecimal roomFee,
                                     int nights, LocalDate checkin, LocalDate today) {
        List<Promotion> found = promotionMapper.selectList(
                new LambdaQueryWrapper<Promotion>().eq(Promotion::getCouponCode, code));
        if (found.isEmpty()) {
            throw new BusinessException("优惠码无效");
        }
        Promotion p = found.get(0);
        if (p.getStatus() == null || p.getStatus() != 1) {
            throw new BusinessException("优惠码已停用");
        }
        if (today.isBefore(p.getStartDate()) || today.isAfter(p.getEndDate())) {
            throw new BusinessException("优惠码已过期");
        }
        if (p.getRoomId() != null && !p.getRoomId().equals(roomId)) {
            throw new BusinessException("该优惠码不适用于此房间");
        }
        if (p.getUsageLimit() != null && p.getUsedCount() != null && p.getUsedCount() >= p.getUsageLimit()) {
            throw new BusinessException("优惠码已被抢完");
        }
        int type = p.getType() == null ? 0 : p.getType();
        if (type == 2 && p.getThresholdAmount() != null && roomFee.compareTo(p.getThresholdAmount()) < 0) {
            throw new BusinessException("未满足满减门槛(满" + p.getThresholdAmount().stripTrailingZeros().toPlainString() + "可用)");
        }
        if (type == 3 && p.getMinNights() != null && nights < p.getMinNights()) {
            throw new BusinessException("未满足最少连住" + p.getMinNights() + "晚要求");
        }
        if (type == 4 && p.getAdvanceDays() != null && ChronoUnit.DAYS.between(today, checkin) < p.getAdvanceDays()) {
            throw new BusinessException("需提前" + p.getAdvanceDays() + "天预订方可使用");
        }
        return p;
    }

    /** 计算某促销在指定房费小计上的优惠额(封顶为房费) */
    private BigDecimal discountOf(Promotion p, BigDecimal roomFee) {
        int type = p.getType() == null ? 0 : p.getType();
        BigDecimal d = switch (type) {
            case 2 -> p.getDiscountAmount() == null ? BigDecimal.ZERO : p.getDiscountAmount();
            case 1, 3, 4 -> p.getDiscountRate() == null ? BigDecimal.ZERO
                    : roomFee.multiply(BigDecimal.ONE.subtract(p.getDiscountRate()));
            default -> BigDecimal.ZERO;
        };
        d = d.setScale(2, RoundingMode.HALF_UP);
        if (d.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }
        return d.min(roomFee);
    }
}
