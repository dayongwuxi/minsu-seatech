package com.seatech.minsu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.seatech.minsu.dto.IncomeQuery;
import com.seatech.minsu.dto.IncomeVO;
import com.seatech.minsu.dto.PayMethodRatioVO;
import com.seatech.minsu.dto.TrendPointVO;
import com.seatech.minsu.entity.Payment;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface PaymentMapper extends BaseMapper<Payment> {

    /** 收入管理分页(联表订单/房间/会员) */
    @Select("""
            <script>
            SELECT p.id, p.pay_no AS payNo, p.booking_id AS bookingId, p.member_id AS memberId,
                   p.amount, p.pay_method AS payMethod, p.pay_status AS payStatus,
                   p.pay_time AS payTime, p.transaction_no AS transactionNo, p.create_time AS createTime,
                   b.order_no AS orderNo, r.room_name AS roomName,
                   m.name AS memberName, m.username AS memberUsername
            FROM payment p
            LEFT JOIN booking b ON p.booking_id = b.id
            LEFT JOIN room r ON b.room_id = r.id
            LEFT JOIN member m ON p.member_id = m.id
            <where>
              <if test="q.dateStart != null"> AND DATE(p.pay_time) &gt;= #{q.dateStart}</if>
              <if test="q.dateEnd != null"> AND DATE(p.pay_time) &lt;= #{q.dateEnd}</if>
              <if test="q.roomName != null and q.roomName != ''"> AND r.room_name LIKE CONCAT('%', #{q.roomName}, '%')</if>
              <if test="q.payMethod != null"> AND p.pay_method = #{q.payMethod}</if>
              <if test="q.payStatus != null"> AND p.pay_status = #{q.payStatus}</if>
            </where>
            ORDER BY p.create_time DESC
            </script>
            """)
    IPage<IncomeVO> selectIncomePage(Page<IncomeVO> page, @Param("q") IncomeQuery q);

    @Select("SELECT IFNULL(SUM(amount), 0) FROM payment WHERE pay_status = 1 AND DATE(pay_time) = #{date}")
    BigDecimal sumIncomeByDate(@Param("date") LocalDate date);

    @Select("SELECT IFNULL(SUM(amount), 0) FROM payment WHERE pay_status = 1 AND pay_time >= #{start} AND pay_time < #{end}")
    BigDecimal sumIncomeBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /** 累计收入(含已退款的历史成功支付) */
    @Select("SELECT IFNULL(SUM(amount), 0) FROM payment WHERE pay_status IN (1, 3)")
    BigDecimal sumTotalIncome();

    @Select("SELECT IFNULL(SUM(amount), 0) FROM payment WHERE pay_status = 3")
    BigDecimal sumRefund();

    /** 收入趋势, fmt: %Y-%m-%d 或 %Y-%m */
    @Select("""
            SELECT DATE_FORMAT(pay_time, #{fmt}) AS date, IFNULL(SUM(amount), 0) AS value
            FROM payment WHERE pay_status = 1 AND pay_time >= #{start}
            GROUP BY DATE_FORMAT(pay_time, #{fmt}) ORDER BY date
            """)
    List<TrendPointVO> selectIncomeTrend(@Param("start") LocalDateTime start, @Param("fmt") String fmt);

    @Select("""
            SELECT pay_method AS payMethod, COUNT(*) AS count, IFNULL(SUM(amount), 0) AS amount
            FROM payment WHERE pay_status IN (1, 3)
            GROUP BY pay_method
            """)
    List<PayMethodRatioVO> selectPayMethodRatio();
}
