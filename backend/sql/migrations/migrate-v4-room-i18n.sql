-- =====================================================================
-- V4 迁移: 房间说明/预订须知 多语言译文 (2026-07-22)
-- base 中文仍存 room.description / room.booking_note；本表存 AI 翻译的其他语种。
-- 对已运行库执行本文件；全新环境 schema.sql 已并入。
-- 执行: docker exec -i minsu-mysql mysql -uminsu -pminsu123 minsu < 本文件
-- =====================================================================
SET NAMES utf8mb4;
USE minsu;

CREATE TABLE IF NOT EXISTS room_i18n (
  id           BIGINT PRIMARY KEY AUTO_INCREMENT,
  room_id      BIGINT NOT NULL,
  lang         VARCHAR(10) NOT NULL COMMENT '语言码 en/ja/ko/fr/de/es/pt/ru',
  description  VARCHAR(2000) COMMENT '该语言房间说明',
  booking_note VARCHAR(1000) COMMENT '该语言预订须知',
  update_time  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_room_lang (room_id, lang)
) COMMENT '房间说明/须知多语言译文(base 中文在 room 表)';
