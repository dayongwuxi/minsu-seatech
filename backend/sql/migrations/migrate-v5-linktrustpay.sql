-- =====================================================================
-- V5 迁移: LinkTrust Pay 跳转支付渠道接入 (2026-07-24)
-- 设计见 docs/LinkTrustPay接入设计.md
-- 无新表新列（merOrderId 复用 payment.pay_no、渠道交易号复用 transaction_no），
-- 仅更新枚举注释口径：channel 增 3LinkTrust、pay_method 增 7银联。
-- =====================================================================
SET NAMES utf8mb4;
USE minsu;

ALTER TABLE payment
  MODIFY COLUMN pay_method TINYINT NOT NULL COMMENT '1微信 2支付宝 3银行卡 4其他/余额 5PayPay 6PayPal 7银联',
  MODIFY COLUMN channel TINYINT NOT NULL DEFAULT 1 COMMENT '1模拟 2Stripe 3LinkTrust';

ALTER TABLE refund_record
  MODIFY COLUMN channel TINYINT NOT NULL DEFAULT 1 COMMENT '1模拟 2Stripe 3LinkTrust';
