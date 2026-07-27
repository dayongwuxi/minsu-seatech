package com.seatech.minsu.service;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * PriceService 定价规则单元测试（mock mapper 层）。
 * 固定场景：房价 500/晚，会员折扣 0.95（member_type id=1）。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PriceServiceTest {

    private static final Long MEMBER_ID = 1L;
    private static final Long ROOM_ID = 10L;

    @Mock
    private RoomMapper roomMapper;
    @Mock
    private MemberMapper memberMapper;
    @Mock
    private MemberTypeMapper memberTypeMapper;
    @Mock
    private PromotionMapper promotionMapper;

    private PriceService priceService;

    private final LocalDate today = LocalDate.now();

    @BeforeEach
    void setUp() {
        StripeConfig stripeConfig = new StripeConfig("", "", "", "cny", "card");
        priceService = new PriceService(roomMapper, memberMapper, memberTypeMapper, promotionMapper, stripeConfig);

        Room room = new Room();
        room.setId(ROOM_ID);
        room.setPrice(new BigDecimal("500.00"));
        room.setShelfStatus(1);
        when(roomMapper.selectById(ROOM_ID)).thenReturn(room);

        Member member = new Member();
        member.setId(MEMBER_ID);
        member.setMemberTypeId(1L);
        when(memberMapper.selectById(MEMBER_ID)).thenReturn(member);

        MemberType type = new MemberType();
        type.setId(1L);
        type.setDiscount(new BigDecimal("0.95"));
        when(memberTypeMapper.selectById(1L)).thenReturn(type);

        // 默认无任何促销
        when(promotionMapper.selectList(any())).thenReturn(List.of());
    }

    private Promotion promo(int type) {
        Promotion p = new Promotion();
        p.setId(100L + type);
        p.setName("促销" + type);
        p.setType(type);
        p.setStartDate(today.minusDays(1));
        p.setEndDate(today.plusDays(90));
        p.setStatus(1);
        p.setUsedCount(0);
        return p;
    }

    // ---------- 无促销：仅会员折扣 ----------

    @Test
    @DisplayName("无促销时仅会员折扣: 1000×5% = 50, 总额950")
    void quoteWithoutPromotion() {
        QuoteVO vo = priceService.quote(MEMBER_ID, ROOM_ID, today.plusDays(1), today.plusDays(3), null);
        assertEquals(2, vo.getNights().intValue());
        assertEquals(0, vo.getRoomFee().compareTo(new BigDecimal("1000.00")));
        assertEquals(0, vo.getPromoDiscount().compareTo(BigDecimal.ZERO));
        assertEquals(0, vo.getMemberDiscount().compareTo(new BigDecimal("50.00")));
        assertEquals(0, vo.getTotalAmount().compareTo(new BigDecimal("950.00")));
        assertEquals("cny", vo.getCurrency());
        assertNull(vo.getPromotionId());
    }

    // ---------- 4 种促销类型 ----------

    @Test
    @DisplayName("type=1 百分比折扣(自动): 1000×(1-0.8)=200, 会员折扣基于800")
    void percentagePromotion() {
        Promotion p = promo(1);
        p.setDiscountRate(new BigDecimal("0.80"));
        when(promotionMapper.selectList(any())).thenReturn(List.of(p));

        QuoteVO vo = priceService.quote(MEMBER_ID, ROOM_ID, today.plusDays(1), today.plusDays(3), null);
        assertEquals(0, vo.getPromoDiscount().compareTo(new BigDecimal("200.00")));
        // 会员折扣叠加顺序：基于(1000-200)=800 计算 5% = 40
        assertEquals(0, vo.getMemberDiscount().compareTo(new BigDecimal("40.00")));
        assertEquals(0, vo.getTotalAmount().compareTo(new BigDecimal("760.00")));
        assertEquals(p.getId(), vo.getPromotionId());
        assertEquals("促销1", vo.getPromoName());
    }

    @Test
    @DisplayName("type=2 满减(优惠码): 满1000减100")
    void fullReductionCoupon() {
        Promotion p = promo(2);
        p.setCouponCode("SUMMER100");
        p.setThresholdAmount(new BigDecimal("1000.00"));
        p.setDiscountAmount(new BigDecimal("100.00"));
        // 第一次调用返回自动促销(空)，第二次返回优惠码查询结果
        when(promotionMapper.selectList(any())).thenReturn(List.of(), List.of(p));

        QuoteVO vo = priceService.quote(MEMBER_ID, ROOM_ID, today.plusDays(1), today.plusDays(3), "SUMMER100");
        assertEquals(0, vo.getPromoDiscount().compareTo(new BigDecimal("100.00")));
        assertEquals(0, vo.getMemberDiscount().compareTo(new BigDecimal("45.00")));
        assertEquals(0, vo.getTotalAmount().compareTo(new BigDecimal("855.00")));
        assertEquals("SUMMER100", vo.getPromoCode());
    }

    @Test
    @DisplayName("type=3 长住折扣: 连住7晚打9折; 不足晚数自动跳过")
    void longStayPromotion() {
        Promotion p = promo(3);
        p.setDiscountRate(new BigDecimal("0.90"));
        p.setMinNights(7);
        when(promotionMapper.selectList(any())).thenReturn(List.of(p));

        // 7 晚：3500×10% = 350
        QuoteVO vo = priceService.quote(MEMBER_ID, ROOM_ID, today.plusDays(1), today.plusDays(8), null);
        assertEquals(0, vo.getPromoDiscount().compareTo(new BigDecimal("350.00")));

        // 6 晚：自动促销静默跳过
        QuoteVO vo2 = priceService.quote(MEMBER_ID, ROOM_ID, today.plusDays(1), today.plusDays(7), null);
        assertEquals(0, vo2.getPromoDiscount().compareTo(BigDecimal.ZERO));
        assertNull(vo2.getPromotionId());
    }

    @Test
    @DisplayName("type=4 早鸟折扣: 提前30天95折; 提前不足自动跳过")
    void earlyBirdPromotion() {
        Promotion p = promo(4);
        p.setDiscountRate(new BigDecimal("0.95"));
        p.setAdvanceDays(30);
        when(promotionMapper.selectList(any())).thenReturn(List.of(p));

        // 提前 40 天：1000×5% = 50
        QuoteVO vo = priceService.quote(MEMBER_ID, ROOM_ID, today.plusDays(40), today.plusDays(42), null);
        assertEquals(0, vo.getPromoDiscount().compareTo(new BigDecimal("50.00")));

        // 提前 5 天：跳过
        QuoteVO vo2 = priceService.quote(MEMBER_ID, ROOM_ID, today.plusDays(5), today.plusDays(7), null);
        assertEquals(0, vo2.getPromoDiscount().compareTo(BigDecimal.ZERO));
    }

    // ---------- 优惠码异常场景 ----------

    @Test
    @DisplayName("优惠码门槛不足: 抛出具体原因")
    void couponThresholdNotMet() {
        Promotion p = promo(2);
        p.setCouponCode("SUMMER100");
        p.setThresholdAmount(new BigDecimal("1000.00"));
        p.setDiscountAmount(new BigDecimal("100.00"));
        when(promotionMapper.selectList(any())).thenReturn(List.of(), List.of(p));

        // 1 晚 500 < 门槛 1000
        BusinessException e = assertThrows(BusinessException.class,
                () -> priceService.quote(MEMBER_ID, ROOM_ID, today.plusDays(1), today.plusDays(2), "SUMMER100"));
        assertTrue(e.getMessage().contains("门槛"));
    }

    @Test
    @DisplayName("优惠码限量已抢完: 抛出具体原因")
    void couponSoldOut() {
        Promotion p = promo(2);
        p.setCouponCode("SUMMER100");
        p.setThresholdAmount(new BigDecimal("500.00"));
        p.setDiscountAmount(new BigDecimal("100.00"));
        p.setUsageLimit(500);
        p.setUsedCount(500);
        when(promotionMapper.selectList(any())).thenReturn(List.of(), List.of(p));

        BusinessException e = assertThrows(BusinessException.class,
                () -> priceService.quote(MEMBER_ID, ROOM_ID, today.plusDays(1), today.plusDays(3), "SUMMER100"));
        assertTrue(e.getMessage().contains("抢完"));
    }

    @Test
    @DisplayName("优惠码无效: 查无此码")
    void couponNotFound() {
        when(promotionMapper.selectList(any())).thenReturn(List.of(), List.of());
        BusinessException e = assertThrows(BusinessException.class,
                () -> priceService.quote(MEMBER_ID, ROOM_ID, today.plusDays(1), today.plusDays(3), "NOPE"));
        assertTrue(e.getMessage().contains("无效"));
    }

    @Test
    @DisplayName("优惠码不适用该房间")
    void couponWrongRoom() {
        Promotion p = promo(2);
        p.setCouponCode("SUMMER100");
        p.setThresholdAmount(new BigDecimal("500.00"));
        p.setDiscountAmount(new BigDecimal("100.00"));
        p.setRoomId(999L);
        when(promotionMapper.selectList(any())).thenReturn(List.of(), List.of(p));

        BusinessException e = assertThrows(BusinessException.class,
                () -> priceService.quote(MEMBER_ID, ROOM_ID, today.plusDays(1), today.plusDays(3), "SUMMER100"));
        assertTrue(e.getMessage().contains("不适用"));
    }

    @Test
    @DisplayName("优惠码已过期")
    void couponExpired() {
        Promotion p = promo(2);
        p.setCouponCode("OLD");
        p.setThresholdAmount(new BigDecimal("500.00"));
        p.setDiscountAmount(new BigDecimal("100.00"));
        p.setStartDate(today.minusDays(60));
        p.setEndDate(today.minusDays(1));
        when(promotionMapper.selectList(any())).thenReturn(List.of(), List.of(p));

        BusinessException e = assertThrows(BusinessException.class,
                () -> priceService.quote(MEMBER_ID, ROOM_ID, today.plusDays(1), today.plusDays(3), "OLD"));
        assertTrue(e.getMessage().contains("过期"));
    }

    // ---------- 组合规则 ----------

    @Test
    @DisplayName("多促销同时命中: 取优惠额最大的一条")
    void multiplePromotionsPickMax() {
        Promotion small = promo(1);
        small.setId(101L);
        small.setDiscountRate(new BigDecimal("0.90")); // 1000×10% = 100
        Promotion big = promo(2);
        big.setId(102L);
        big.setThresholdAmount(new BigDecimal("500.00"));
        big.setDiscountAmount(new BigDecimal("150.00")); // 150 更大
        when(promotionMapper.selectList(any())).thenReturn(List.of(small, big));

        QuoteVO vo = priceService.quote(MEMBER_ID, ROOM_ID, today.plusDays(1), today.plusDays(3), null);
        assertEquals(0, vo.getPromoDiscount().compareTo(new BigDecimal("150.00")));
        assertEquals(Long.valueOf(102L), vo.getPromotionId());
    }

    @Test
    @DisplayName("限量抢完的自动促销被跳过, 落到次优促销")
    void exhaustedAutoPromotionSkipped() {
        Promotion exhausted = promo(1);
        exhausted.setId(101L);
        exhausted.setDiscountRate(new BigDecimal("0.50"));
        exhausted.setUsageLimit(10);
        exhausted.setUsedCount(10);
        Promotion ok = promo(1);
        ok.setId(102L);
        ok.setDiscountRate(new BigDecimal("0.90"));
        when(promotionMapper.selectList(any())).thenReturn(List.of(exhausted, ok));

        QuoteVO vo = priceService.quote(MEMBER_ID, ROOM_ID, today.plusDays(1), today.plusDays(3), null);
        assertEquals(Long.valueOf(102L), vo.getPromotionId());
        assertEquals(0, vo.getPromoDiscount().compareTo(new BigDecimal("100.00")));
    }

    @Test
    @DisplayName("会员折扣叠加顺序: 基于扣除促销后的余额, 不复利")
    void memberDiscountAppliedAfterPromotion() {
        Promotion p = promo(2);
        p.setThresholdAmount(new BigDecimal("500.00"));
        p.setDiscountAmount(new BigDecimal("400.00"));
        when(promotionMapper.selectList(any())).thenReturn(List.of(p));

        QuoteVO vo = priceService.quote(MEMBER_ID, ROOM_ID, today.plusDays(1), today.plusDays(3), null);
        // 会员折扣 = (1000-400)×5% = 30, 而非 1000×5% = 50
        assertEquals(0, vo.getMemberDiscount().compareTo(new BigDecimal("30.00")));
        assertEquals(0, vo.getTotalAmount().compareTo(new BigDecimal("570.00")));
    }

    @Test
    @DisplayName("总额下限 0.01: 促销覆盖全部房费时不出现 0 或负数")
    void totalAmountFloor() {
        Promotion p = promo(2);
        p.setThresholdAmount(new BigDecimal("100.00"));
        p.setDiscountAmount(new BigDecimal("99999.00")); // 超过房费, 应封顶
        when(promotionMapper.selectList(any())).thenReturn(List.of(p));

        QuoteVO vo = priceService.quote(MEMBER_ID, ROOM_ID, today.plusDays(1), today.plusDays(2), null);
        // 优惠封顶为房费 500
        assertEquals(0, vo.getPromoDiscount().compareTo(new BigDecimal("500.00")));
        assertEquals(0, vo.getTotalAmount().compareTo(new BigDecimal("0.01")));
    }

    @Test
    @DisplayName("零小数货币(jpy)总额下限为 1：避免向渠道发送 amount=0")
    void totalAmountFloorIsOneForZeroDecimalCurrency() {
        StripeConfig jpyConfig = new StripeConfig("", "", "", "jpy", "card");
        PriceService jpyService = new PriceService(
                roomMapper, memberMapper, memberTypeMapper, promotionMapper, jpyConfig);
        Promotion p = promo(2);
        p.setThresholdAmount(new BigDecimal("100.00"));
        p.setDiscountAmount(new BigDecimal("99999.00"));
        when(promotionMapper.selectList(any())).thenReturn(List.of(p));

        QuoteVO vo = jpyService.quote(MEMBER_ID, ROOM_ID, today.plusDays(1), today.plusDays(2), null);
        assertEquals(0, vo.getTotalAmount().compareTo(BigDecimal.ONE));
        assertEquals("jpy", vo.getCurrency());
    }

    @Test
    @DisplayName("非会员(查无此人)不享受会员折扣")
    void noMemberNoDiscount() {
        when(memberMapper.selectById(99L)).thenReturn(null);
        QuoteVO vo = priceService.quote(99L, ROOM_ID, today.plusDays(1), today.plusDays(3), null);
        assertEquals(0, vo.getMemberDiscount().compareTo(BigDecimal.ZERO));
        assertEquals(0, vo.getTotalAmount().compareTo(new BigDecimal("1000.00")));
    }
}
