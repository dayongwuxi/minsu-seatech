-- =====================================================================
-- V7 迁移: 支付并发闸门 (2026-07-24)
-- 同一订单+渠道最多一条待支付流水（防并发双击产生双 merOrderId → 双扣）。
-- 生成列 pending_flag: pay_status=0 → 1，否则 NULL（NULL 不参与唯一冲突，
-- 已结算历史行不受限）。代码侧撞约束时回落复用已建行。
-- 房源双预订闸门为下单事务内 SELECT ... FOR UPDATE 行锁，无 DDL。
-- =====================================================================
SET NAMES utf8mb4;
USE minsu;

-- 历史多余待支付行清理：同 booking+channel 仅保留最新一条，其余置失败
UPDATE payment p
JOIN (
  SELECT booking_id, channel, MAX(id) AS keep_id
  FROM payment WHERE pay_status = 0
  GROUP BY booking_id, channel
) k ON p.booking_id = k.booking_id AND p.channel = k.channel
SET p.pay_status = 2
WHERE p.pay_status = 0 AND p.id <> k.keep_id;

ALTER TABLE payment
  ADD COLUMN pending_flag TINYINT GENERATED ALWAYS AS (IF(pay_status = 0, 1, NULL)) VIRTUAL COMMENT '并发闸门: 待支付=1 其余NULL',
  ADD UNIQUE KEY uk_payment_pending (booking_id, channel, pending_flag);
