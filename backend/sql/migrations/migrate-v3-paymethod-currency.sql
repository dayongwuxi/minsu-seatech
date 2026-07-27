-- =====================================================================
-- V3 迁移: 支付方式管理(绑卡) + 多货币显示汇率 (2026-07-08)
-- 设计见 docs/支付促销退款设计.md 第 8/9 节
-- =====================================================================
SET NAMES utf8mb4;
USE minsu;

-- 会员绑定 Stripe Customer
ALTER TABLE member
  ADD COLUMN stripe_customer_id VARCHAR(64) COMMENT 'Stripe Customer id' AFTER member_type_id;

-- 已保存支付方式(卡号等敏感信息只存于 Stripe, 本表仅缓存展示元数据)
CREATE TABLE member_payment_method (
  id           BIGINT PRIMARY KEY AUTO_INCREMENT,
  member_id    BIGINT NOT NULL,
  channel      TINYINT NOT NULL DEFAULT 1 COMMENT '1模拟 2Stripe',
  stripe_payment_method_id VARCHAR(64) COMMENT 'Stripe PaymentMethod id (channel=2)',
  method_type  TINYINT NOT NULL DEFAULT 1 COMMENT '1银行卡 2其他',
  brand        VARCHAR(20) COMMENT '卡品牌 visa/mastercard/jcb/unionpay',
  last4        VARCHAR(4)  COMMENT '卡号后四位',
  exp_month    TINYINT,
  exp_year     SMALLINT,
  holder_name  VARCHAR(50) COMMENT '持卡人(可选)',
  is_default   TINYINT NOT NULL DEFAULT 0 COMMENT '默认支付方式',
  deleted      TINYINT NOT NULL DEFAULT 0,
  create_time  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_mpm_member (member_id, deleted)
) COMMENT '会员已保存支付方式';

-- 展示汇率(基准 CNY): 1 CNY = rate 单位目标货币
CREATE TABLE exchange_rate (
  id            BIGINT PRIMARY KEY AUTO_INCREMENT,
  currency_code VARCHAR(8) NOT NULL UNIQUE COMMENT 'USD/EUR/GBP/JPY/CNY',
  currency_name VARCHAR(50) NOT NULL,
  symbol        VARCHAR(8) NOT NULL,
  rate          DECIMAL(16,8) NOT NULL COMMENT '1 CNY 兑换量',
  auto_update   TINYINT NOT NULL DEFAULT 1 COMMENT '1随API自动刷新 0管理员手工锁定',
  source        TINYINT NOT NULL DEFAULT 1 COMMENT '1 API 2 手工',
  status        TINYINT NOT NULL DEFAULT 1 COMMENT '0停用 1启用',
  update_time   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT '展示汇率(结算一律 CNY)';

-- 初始汇率种子(近似值, 启动后由 open.er-api.com 自动刷新 auto_update=1 的行)
INSERT INTO exchange_rate (currency_code, currency_name, symbol, rate, auto_update, source) VALUES
 ('CNY','人民币','¥',1.00000000,0,2),
 ('USD','美元','$',0.14000000,1,1),
 ('EUR','欧元','€',0.12000000,1,1),
 ('GBP','英镑','£',0.10500000,1,1),
 ('JPY','日元','JP¥',21.00000000,1,1);
