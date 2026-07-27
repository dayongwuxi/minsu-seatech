package com.seatech.minsu.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.seatech.minsu.common.BusinessException;
import com.seatech.minsu.common.ResultCode;
import com.seatech.minsu.config.StripeConfig;
import com.seatech.minsu.dto.PaymentMethodAddRequest;
import com.seatech.minsu.dto.SetupIntentVO;
import com.seatech.minsu.entity.Member;
import com.seatech.minsu.entity.MemberPaymentMethod;
import com.seatech.minsu.mapper.MemberMapper;
import com.seatech.minsu.mapper.MemberPaymentMethodMapper;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.PaymentMethod;
import com.stripe.model.SetupIntent;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.SetupIntentCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 支付方式管理（绑卡）。敏感数据零落库：
 * Stripe 通道走 SetupIntent，本地仅缓存 brand/last4/有效期；Mock 通道禁止接收完整卡号。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentMethodService {

    private final MemberPaymentMethodMapper methodMapper;
    private final MemberMapper memberMapper;
    private final StripeConfig stripeConfig;

    /** 当前会员支付方式列表，默认在前 */
    public List<MemberPaymentMethod> listByMember(Long memberId) {
        return methodMapper.selectList(new LambdaQueryWrapper<MemberPaymentMethod>()
                .eq(MemberPaymentMethod::getMemberId, memberId)
                .orderByDesc(MemberPaymentMethod::getIsDefault)
                .orderByAsc(MemberPaymentMethod::getId));
    }

    /** 绑卡 SetupIntent：首次自动创建 Stripe Customer；Stripe 未启用返回 mock=true */
    public SetupIntentVO createSetupIntent(Long memberId) {
        SetupIntentVO vo = new SetupIntentVO();
        if (!stripeConfig.isEnabled()) {
            vo.setMock(true);
            return vo;
        }
        Member member = memberMapper.selectById(memberId);
        if (member == null) {
            throw new BusinessException("用户不存在");
        }
        try {
            String customerId = ensureCustomer(member);
            SetupIntent intent = SetupIntent.create(SetupIntentCreateParams.builder()
                    .setCustomer(customerId)
                    .addPaymentMethodType("card")
                    .build());
            vo.setClientSecret(intent.getClientSecret());
            vo.setMock(false);
            return vo;
        } catch (StripeException e) {
            log.error("创建 SetupIntent 失败: memberId={}", memberId, e);
            throw new BusinessException(ResultCode.SERVER_ERROR, "绑卡渠道异常，请稍后重试");
        }
    }

    /** 保存支付方式：Stripe 模式校验归属并缓存元数据；Mock 模式仅收 brand/last4/有效期 */
    public MemberPaymentMethod add(Long memberId, PaymentMethodAddRequest req) {
        // 敏感数据防御：任何模式下出现完整卡号直接拒绝
        if (req.getCardNumber() != null && req.getCardNumber().replaceAll("\\D", "").length() > 4) {
            throw new BusinessException("禁止提交完整卡号，请仅提交卡号后四位");
        }
        MemberPaymentMethod method = new MemberPaymentMethod();
        method.setMemberId(memberId);
        method.setMethodType(1);

        if (stripeConfig.isEnabled() && StringUtils.hasText(req.getStripePaymentMethodId())) {
            Member member = memberMapper.selectById(memberId);
            if (member == null || !StringUtils.hasText(member.getStripeCustomerId())) {
                throw new BusinessException("请先完成绑卡流程");
            }
            try {
                PaymentMethod pm = PaymentMethod.retrieve(req.getStripePaymentMethodId());
                if (pm.getCustomer() == null || !pm.getCustomer().equals(member.getStripeCustomerId())) {
                    throw new BusinessException("该支付方式未绑定到当前账户");
                }
                method.setChannel(2);
                method.setStripePaymentMethodId(pm.getId());
                PaymentMethod.Card card = pm.getCard();
                if (card != null) {
                    method.setBrand(card.getBrand());
                    method.setLast4(card.getLast4());
                    method.setExpMonth(card.getExpMonth() == null ? null : card.getExpMonth().intValue());
                    method.setExpYear(card.getExpYear() == null ? null : card.getExpYear().intValue());
                }
            } catch (StripeException e) {
                log.error("校验 Stripe PaymentMethod 失败: {}", req.getStripePaymentMethodId(), e);
                throw new BusinessException(ResultCode.SERVER_ERROR, "绑卡渠道异常，请稍后重试");
            }
        } else {
            if (req.getLast4() == null || !req.getLast4().matches("\\d{4}")) {
                throw new BusinessException("请提供卡号后四位");
            }
            if (req.getExpMonth() != null && (req.getExpMonth() < 1 || req.getExpMonth() > 12)) {
                throw new BusinessException("有效期月份不合法");
            }
            method.setChannel(1);
            method.setBrand(StringUtils.hasText(req.getBrand()) ? req.getBrand() : "card");
            method.setLast4(req.getLast4());
            method.setExpMonth(req.getExpMonth());
            method.setExpYear(req.getExpYear());
            method.setHolderName(req.getHolderName());
        }

        // 首张卡自动设为默认
        Long existing = methodMapper.selectCount(new LambdaQueryWrapper<MemberPaymentMethod>()
                .eq(MemberPaymentMethod::getMemberId, memberId));
        method.setIsDefault(existing == null || existing == 0 ? 1 : 0);
        methodMapper.insert(method);
        return method;
    }

    /** 设为默认（同会员互斥） */
    @Transactional(rollbackFor = Exception.class)
    public void setDefault(Long memberId, Long id) {
        MemberPaymentMethod method = requireOwned(memberId, id);
        methodMapper.update(null, new LambdaUpdateWrapper<MemberPaymentMethod>()
                .eq(MemberPaymentMethod::getMemberId, memberId)
                .set(MemberPaymentMethod::getIsDefault, 0));
        method.setIsDefault(1);
        methodMapper.updateById(method);
    }

    /** 删除：Stripe 通道先 detach（失败仅记日志仍软删）；删默认卡时最早一张补位 */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long memberId, Long id) {
        MemberPaymentMethod method = requireOwned(memberId, id);
        if (method.getChannel() != null && method.getChannel() == 2
                && stripeConfig.isEnabled()
                && StringUtils.hasText(method.getStripePaymentMethodId())) {
            try {
                PaymentMethod.retrieve(method.getStripePaymentMethodId()).detach();
            } catch (StripeException e) {
                log.warn("Stripe PaymentMethod detach 失败(继续本地删除): {}", method.getStripePaymentMethodId(), e);
            }
        }
        methodMapper.deleteById(id);
        if (method.getIsDefault() != null && method.getIsDefault() == 1) {
            List<MemberPaymentMethod> rest = methodMapper.selectList(
                    new LambdaQueryWrapper<MemberPaymentMethod>()
                            .eq(MemberPaymentMethod::getMemberId, memberId)
                            .orderByAsc(MemberPaymentMethod::getId));
            if (!rest.isEmpty()) {
                MemberPaymentMethod earliest = rest.get(0);
                earliest.setIsDefault(1);
                methodMapper.updateById(earliest);
            }
        }
    }

    /** 校验归属并返回（供支付下单选卡使用） */
    public MemberPaymentMethod requireOwned(Long memberId, Long id) {
        MemberPaymentMethod method = methodMapper.selectById(id);
        if (method == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "支付方式不存在");
        }
        if (!method.getMemberId().equals(memberId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权操作该支付方式");
        }
        return method;
    }

    private String ensureCustomer(Member member) throws StripeException {
        if (StringUtils.hasText(member.getStripeCustomerId())) {
            return member.getStripeCustomerId();
        }
        CustomerCreateParams.Builder builder = CustomerCreateParams.builder()
                .putMetadata("memberId", String.valueOf(member.getId()));
        if (StringUtils.hasText(member.getEmail())) {
            builder.setEmail(member.getEmail());
        }
        builder.setName(StringUtils.hasText(member.getName()) ? member.getName() : member.getUsername());
        Customer customer = Customer.create(builder.build());
        member.setStripeCustomerId(customer.getId());
        memberMapper.updateById(member);
        return customer.getId();
    }
}
