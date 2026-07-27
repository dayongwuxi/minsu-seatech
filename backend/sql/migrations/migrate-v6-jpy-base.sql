-- =====================================================================
-- V6 迁移: 站点结算货币 CNY → JPY (2026-07-24，配合 LinkTrustPay 启用)
-- 设计见 docs/LinkTrustPay接入设计.md。金额按当日汇率 1 CNY = 24.133485 JPY
-- 换算：房价取整到百位日元，满减档取整到千位日元。
-- 汇率表基准行切为 JPY(锁定 rate=1)；其余行由启动刷新任务按新基准自动覆盖。
-- 注意：历史 booking/payment 行保留原 currency 标签(cny)，收入统计混算需人工甄别。
-- =====================================================================
SET NAMES utf8mb4;
USE minsu;

UPDATE room SET
  price = ROUND(price * 24.133485, -2),
  origin_price = ROUND(origin_price * 24.133485, -2);

UPDATE promotion SET
  threshold_amount = ROUND(threshold_amount * 24.133485, -3),
  discount_amount = ROUND(discount_amount * 24.133485, -2),
  name = REPLACE(name, '满1000减100', '满24000减2400')
WHERE type = 2;

-- 基准行：JPY 锁定为 1（对齐原 CNY 基准行的 auto_update=0/source=2 约定）
UPDATE exchange_rate SET rate = 1.00000000, auto_update = 0, source = 2 WHERE currency_code = 'JPY';
-- CNY 转为普通展示币种，交给 API 自动刷新
UPDATE exchange_rate SET auto_update = 1, source = 1 WHERE currency_code = 'CNY';
