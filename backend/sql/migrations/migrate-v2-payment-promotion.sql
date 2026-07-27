-- =====================================================================
-- V2 迁移: 支付/促销/退款域 (2026-07-08, 设计见 docs/支付促销退款设计.md)
-- 对已运行库执行本文件; 全新环境直接用 schema.sql (已并入本变更)
-- =====================================================================
SET NAMES utf8mb4;
USE minsu;

CREATE TABLE promotion (
  id              BIGINT PRIMARY KEY AUTO_INCREMENT,
  promo_no        VARCHAR(30) NOT NULL UNIQUE COMMENT '促销编号 PM+yyyyMMdd+4位流水',
  name            VARCHAR(100) NOT NULL COMMENT '活动名称(价格明细中展示)',
  type            TINYINT NOT NULL COMMENT '1百分比折扣 2满减 3长住折扣 4早鸟折扣',
  discount_rate   DECIMAL(4,3) COMMENT '折扣率 0.80=8折 (type=1/3/4)',
  discount_amount DECIMAL(10,2) COMMENT '立减金额 (type=2)',
  threshold_amount DECIMAL(10,2) COMMENT '满减门槛 (type=2)',
  min_nights      INT COMMENT '最少连住晚数 (type=3)',
  advance_days    INT COMMENT '提前预订天数 (type=4)',
  room_id         BIGINT COMMENT '限定房间, NULL=全场',
  coupon_code     VARCHAR(30) COMMENT '优惠码, NULL=自动应用',
  start_date      DATE NOT NULL,
  end_date        DATE NOT NULL,
  usage_limit     INT COMMENT '总使用次数上限, NULL=不限',
  used_count      INT NOT NULL DEFAULT 0,
  status          TINYINT NOT NULL DEFAULT 1 COMMENT '0停用 1启用',
  deleted         TINYINT NOT NULL DEFAULT 0,
  create_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_promo_active (status, start_date, end_date),
  KEY idx_promo_code (coupon_code)
) COMMENT '促销活动';

ALTER TABLE booking
  ADD COLUMN room_fee        DECIMAL(10,2) COMMENT '房费小计=单价×晚数' AFTER unit_price,
  ADD COLUMN promo_discount  DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '促销优惠额' AFTER room_fee,
  ADD COLUMN member_discount DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '会员折扣额' AFTER promo_discount,
  ADD COLUMN promotion_id    BIGINT COMMENT '命中的促销活动id' AFTER member_discount,
  ADD COLUMN promo_code      VARCHAR(30) COMMENT '使用的优惠码' AFTER promotion_id,
  ADD COLUMN currency        VARCHAR(8) NOT NULL DEFAULT 'cny' AFTER total_amount;

-- 兼容旧数据: room_fee 回填
UPDATE booking SET room_fee = unit_price * nights WHERE room_fee IS NULL;

ALTER TABLE payment
  ADD COLUMN channel  TINYINT NOT NULL DEFAULT 1 COMMENT '1模拟 2Stripe' AFTER pay_method,
  ADD COLUMN currency VARCHAR(8) NOT NULL DEFAULT 'cny' AFTER amount,
  ADD COLUMN stripe_payment_intent_id VARCHAR(64) COMMENT 'Stripe PaymentIntent id' AFTER transaction_no,
  ADD KEY idx_pay_intent (stripe_payment_intent_id);

ALTER TABLE refund_record
  ADD COLUMN channel          TINYINT NOT NULL DEFAULT 1 COMMENT '1模拟 2Stripe' AFTER status,
  ADD COLUMN stripe_refund_id VARCHAR(64) COMMENT 'Stripe Refund id' AFTER channel;

-- 种子促销示例
INSERT INTO promotion (promo_no, name, type, discount_rate, min_nights, start_date, end_date, status) VALUES
 ('PM202607080001','长住 7 晚 9 折', 3, 0.900, 7, '2026-07-01', '2027-06-30', 1);
INSERT INTO promotion (promo_no, name, type, discount_rate, advance_days, start_date, end_date, status) VALUES
 ('PM202607080002','提前 30 天早鸟 95 折', 4, 0.950, 30, '2026-07-01', '2027-06-30', 1);
INSERT INTO promotion (promo_no, name, type, discount_amount, threshold_amount, coupon_code, start_date, end_date, usage_limit, status) VALUES
 ('PM202607080003','夏季优惠码 SUMMER100 满1000减100', 2, 100.00, 1000.00, 'SUMMER100', '2026-07-01', '2026-09-30', 500, 1);
