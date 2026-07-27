package com.seatech.minsu.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.seatech.minsu.common.BusinessException;
import com.seatech.minsu.common.ResultCode;
import com.seatech.minsu.config.StripeConfig;
import com.seatech.minsu.dto.PaymentMethodAddRequest;
import com.seatech.minsu.entity.MemberPaymentMethod;
import com.seatech.minsu.mapper.MemberMapper;
import com.seatech.minsu.mapper.MemberPaymentMethodMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** PaymentMethodService 单元测试（Mock 通道路径, mapper 层 mock） */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PaymentMethodServiceTest {

    private static final Long MEMBER_ID = 1L;

    @Mock
    private MemberPaymentMethodMapper methodMapper;
    @Mock
    private MemberMapper memberMapper;

    private PaymentMethodService service;

    @BeforeAll
    static void initTableInfo() {
        // LambdaUpdateWrapper.set() 需要 MP 实体元数据缓存, 纯 Mockito 环境手动初始化
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), MemberPaymentMethod.class);
    }

    @BeforeEach
    void setUp() {
        // secretKey 为空 = Stripe 未启用(Mock 模式)
        StripeConfig stripeConfig = new StripeConfig("", "", "", "cny", "card");
        service = new PaymentMethodService(methodMapper, memberMapper, stripeConfig);
        when(methodMapper.insert(any(MemberPaymentMethod.class))).thenReturn(1);
        when(methodMapper.updateById(any(MemberPaymentMethod.class))).thenReturn(1);
    }

    private MemberPaymentMethod card(Long id, Long memberId, int isDefault) {
        MemberPaymentMethod m = new MemberPaymentMethod();
        m.setId(id);
        m.setMemberId(memberId);
        m.setChannel(1);
        m.setBrand("visa");
        m.setLast4("4242");
        m.setIsDefault(isDefault);
        return m;
    }

    private PaymentMethodAddRequest mockAddReq() {
        PaymentMethodAddRequest req = new PaymentMethodAddRequest();
        req.setBrand("visa");
        req.setLast4("4242");
        req.setExpMonth(12);
        req.setExpYear(2030);
        req.setHolderName("ZHANG SAN");
        return req;
    }

    @Test
    @DisplayName("Mock 模式拒收完整卡号: cardNumber 超过4位数字直接400")
    void rejectFullCardNumber() {
        PaymentMethodAddRequest req = mockAddReq();
        req.setCardNumber("4242 4242 4242 4242");
        BusinessException e = assertThrows(BusinessException.class, () -> service.add(MEMBER_ID, req));
        assertEquals(ResultCode.BAD_REQUEST, e.getCode());
        assertTrue(e.getMessage().contains("卡号"));
        verify(methodMapper, never()).insert(any(MemberPaymentMethod.class));
    }

    @Test
    @DisplayName("cardNumber 恰好等于后四位(4位数字)时不拦截")
    void allowFourDigitCardNumberField() {
        PaymentMethodAddRequest req = mockAddReq();
        req.setCardNumber("4242");
        when(methodMapper.selectCount(any())).thenReturn(0L);
        MemberPaymentMethod saved = service.add(MEMBER_ID, req);
        assertEquals("4242", saved.getLast4());
    }

    @Test
    @DisplayName("首张卡自动 is_default=1, 后续新增默认0")
    void firstCardBecomesDefault() {
        when(methodMapper.selectCount(any())).thenReturn(0L);
        MemberPaymentMethod first = service.add(MEMBER_ID, mockAddReq());
        assertEquals(1, first.getIsDefault().intValue());
        assertEquals(1, first.getChannel().intValue());

        when(methodMapper.selectCount(any())).thenReturn(1L);
        MemberPaymentMethod second = service.add(MEMBER_ID, mockAddReq());
        assertEquals(0, second.getIsDefault().intValue());
    }

    @Test
    @DisplayName("Mock 模式缺少 last4 拒绝")
    void mockModeRequiresLast4() {
        PaymentMethodAddRequest req = mockAddReq();
        req.setLast4("12345");
        assertThrows(BusinessException.class, () -> service.add(MEMBER_ID, req));
        req.setLast4(null);
        assertThrows(BusinessException.class, () -> service.add(MEMBER_ID, req));
    }

    @Test
    @DisplayName("设为默认: 先将同会员全部置0再置1(互斥)")
    void setDefaultMutualExclusive() {
        MemberPaymentMethod target = card(5L, MEMBER_ID, 0);
        when(methodMapper.selectById(5L)).thenReturn(target);

        service.setDefault(MEMBER_ID, 5L);

        // 同会员全部清零
        verify(methodMapper).update(isNull(), any());
        // 目标置1
        ArgumentCaptor<MemberPaymentMethod> captor = ArgumentCaptor.forClass(MemberPaymentMethod.class);
        verify(methodMapper).updateById(captor.capture());
        assertEquals(5L, captor.getValue().getId());
        assertEquals(1, captor.getValue().getIsDefault().intValue());
    }

    @Test
    @DisplayName("删除默认卡: 最早一张自动补位默认")
    void deleteDefaultPromotesEarliest() {
        MemberPaymentMethod target = card(5L, MEMBER_ID, 1);
        when(methodMapper.selectById(5L)).thenReturn(target);
        when(methodMapper.deleteById(5L)).thenReturn(1);
        MemberPaymentMethod earliest = card(7L, MEMBER_ID, 0);
        MemberPaymentMethod later = card(9L, MEMBER_ID, 0);
        when(methodMapper.selectList(any())).thenReturn(List.of(earliest, later));

        service.delete(MEMBER_ID, 5L);

        verify(methodMapper).deleteById(5L);
        ArgumentCaptor<MemberPaymentMethod> captor = ArgumentCaptor.forClass(MemberPaymentMethod.class);
        verify(methodMapper).updateById(captor.capture());
        assertEquals(7L, captor.getValue().getId());
        assertEquals(1, captor.getValue().getIsDefault().intValue());
    }

    @Test
    @DisplayName("删除非默认卡: 不触发补位")
    void deleteNonDefaultNoPromotion() {
        MemberPaymentMethod target = card(5L, MEMBER_ID, 0);
        when(methodMapper.selectById(5L)).thenReturn(target);
        when(methodMapper.deleteById(5L)).thenReturn(1);

        service.delete(MEMBER_ID, 5L);

        verify(methodMapper).deleteById(5L);
        verify(methodMapper, never()).updateById(any(MemberPaymentMethod.class));
    }

    @Test
    @DisplayName("归属校验: 操作他人支付方式返回403")
    void ownershipCheck() {
        MemberPaymentMethod others = card(5L, 999L, 0);
        when(methodMapper.selectById(5L)).thenReturn(others);
        BusinessException e = assertThrows(BusinessException.class, () -> service.delete(MEMBER_ID, 5L));
        assertEquals(ResultCode.FORBIDDEN, e.getCode());
    }
}
