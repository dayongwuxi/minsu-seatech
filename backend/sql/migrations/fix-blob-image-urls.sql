-- =====================================================================
-- 数据修复: 清理误存入库的 el-upload 临时 blob 预览地址 (2026-07-22)
-- 背景: 前端旧逻辑在图片上传失败/未完成时，把浏览器本地 blob: 预览地址
--       当作图片地址存进库，刷新后链接失效、前端看不到图片。
--       代码侧已修复(前端不再收集 blob、后端只接受 /files/ 地址)，
--       本脚本清理历史脏数据。安全: blob: 地址均为死链，删除无损失。
-- 执行: docker exec -i minsu-mysql mysql -uminsu -pminsu123 minsu < 本文件
-- =====================================================================
SET NAMES utf8mb4;
USE minsu;

-- 1) 删除所有 blob: 图片行(死链)
DELETE FROM room_image WHERE image_url LIKE 'blob:%';

-- 2) 封面若是 blob: 死链，重置为该房间剩余的第一张有效图(按 sort)；无图则置空
UPDATE room r
SET cover_image = (
  SELECT ri.image_url FROM room_image ri
  WHERE ri.room_id = r.id
  ORDER BY ri.sort ASC, ri.id ASC
  LIMIT 1
)
WHERE cover_image LIKE 'blob:%';

-- 3) 核对(应均为 0)
SELECT
  (SELECT COUNT(*) FROM room_image WHERE image_url LIKE 'blob:%') AS remaining_blob_images,
  (SELECT COUNT(*) FROM room WHERE cover_image LIKE 'blob:%')     AS remaining_blob_covers;
