#!/bin/bash
# minsu-seatech 一键恢复：从备份 bundle 同时恢复数据库 + 图片卷，并校验对齐。
#
# 用法:
#   restore-minsu.sh --check [bundle]   # 只校验 bundle 完整性/对齐，不动线上数据
#   restore-minsu.sh [bundle]           # 真恢复（交互确认）
#   restore-minsu.sh --yes [bundle]     # 真恢复（免确认，供演练脚本用）
#
# bundle 省略时取本地最新。若本地已无（灾难场景），先从 OSS 下载:
#   /home/steven-amd/backups/minsu/.venv/bin/python -c "见 README 或 sync-minsu-oss.py 的 key 布局"
#   oss://steven-china-storage/db-backup/seatech-minsu/<YYYY>/<MM>/<DD>/minsu_full_*.tar.gz

set -euo pipefail

BACKUP_DIR=/home/steven-amd/backups/minsu
DB_CONTAINER=minsu-mysql
APP_CONTAINER=minsu-backend
DB_USER=minsu
DB_PASS=minsu123
DB_NAME=minsu

CHECK_ONLY=0
ASSUME_YES=0
BUNDLE=""
for arg in "$@"; do
  case "$arg" in
    --check) CHECK_ONLY=1 ;;
    --yes)   ASSUME_YES=1 ;;
    *)       BUNDLE="$arg" ;;
  esac
done

if [ -z "$BUNDLE" ]; then
  BUNDLE=$(ls -1 "$BACKUP_DIR"/minsu_full_*.tar.gz 2>/dev/null | sort | tail -1 || true)
  [ -z "$BUNDLE" ] && { echo "ERROR: 本地没有任何 bundle，请先从 OSS 下载"; exit 2; }
fi
[ -f "$BUNDLE" ] || { echo "ERROR: 找不到 $BUNDLE"; exit 2; }

echo "== Bundle: $BUNDLE"

WORK=$(mktemp -d "$BACKUP_DIR/.restore.XXXXXX")
trap 'rm -rf "$WORK"' EXIT

# ── 1. 解包 + 结构校验 ──
tar -C "$WORK" -xzf "$BUNDLE"
for f in db.sql.gz manifest.json; do
  [ -f "$WORK/$f" ] || { echo "ERROR: bundle 缺少 $f"; exit 2; }
done
[ -d "$WORK/uploads" ] || { echo "ERROR: bundle 缺少 uploads/"; exit 2; }
gzip -t "$WORK/db.sql.gz" || { echo "ERROR: db.sql.gz 损坏"; exit 2; }

# ── 2. manifest 对齐校验：bundle 内文件与 manifest 记录逐一比对 md5 ──
python3 - "$WORK" <<'PYEOF'
import hashlib, json, os, sys
work = sys.argv[1]
m = json.load(open(os.path.join(work, 'manifest.json')))
updir = os.path.join(work, 'uploads')
bad = []
for rel, info in m['files'].items():
    p = os.path.join(updir, rel)
    if not os.path.isfile(p):
        bad.append(f"缺文件 {rel}")
        continue
    h = hashlib.md5(open(p, 'rb').read()).hexdigest()
    if h != info['md5']:
        bad.append(f"md5 不符 {rel}")
if bad:
    print("ERROR: bundle 内图片与 manifest 不一致:", *bad, sep='\n  ')
    sys.exit(2)
print(f"== manifest 校验通过: {m['present_count']} 个图片文件, "
      f"DB 引用 {m['referenced_count']} 个, 备份时缺失 {len(m['missing'])} 个")
if m['missing']:
    print("   WARN 备份时即缺失(恢复后同样缺):", ', '.join(m['missing']))
PYEOF

if [ "$CHECK_ONLY" = 1 ]; then
  echo "== --check 完成，未改动任何线上数据"
  exit 0
fi

# ── 3. 确认 ──
if [ "$ASSUME_YES" != 1 ]; then
  echo "!! 即将用该 bundle 覆盖线上数据库 [$DB_NAME] 和图片卷 /app/uploads"
  read -r -p "!! 输入 yes 继续: " ans
  [ "$ans" = "yes" ] || { echo "已取消"; exit 1; }
fi

for c in "$DB_CONTAINER" "$APP_CONTAINER"; do
  docker ps --format '{{.Names}}' | grep -q "^${c}$" || { echo "ERROR: 容器 $c 未运行"; exit 2; }
done

# ── 4. 恢复数据库（dump 自带 DROP TABLE IF EXISTS，整库原地重建） ──
echo "== 恢复数据库..."
gunzip -c "$WORK/db.sql.gz" | docker exec -i "$DB_CONTAINER" \
  mysql -u"$DB_USER" -p"$DB_PASS" "$DB_NAME" 2>/dev/null
echo "== 数据库恢复完成"

# ── 5. 恢复图片卷（清空后整卷回灌，保证与 DB 同一时间点） ──
echo "== 恢复图片卷..."
docker exec "$APP_CONTAINER" sh -c 'find /app/uploads -mindepth 1 -delete'
tar -C "$WORK" -cf - uploads | docker exec -i "$APP_CONTAINER" tar -C /app -xf -
echo "== 图片卷恢复完成"

# ── 6. 恢复后对齐终检：逐条 DB 引用 → 容器内文件必须存在 ──
echo "== 对齐终检..."
COLS=$(docker exec "$DB_CONTAINER" mysql -u"$DB_USER" -p"$DB_PASS" -N -e \
  "SELECT CONCAT(table_name,'.',column_name) FROM information_schema.columns
   WHERE table_schema='$DB_NAME' AND data_type IN ('varchar','text','mediumtext')
     AND (column_name LIKE '%image%' OR column_name LIKE '%img%'
       OR column_name LIKE '%avatar%' OR column_name LIKE '%url%'
       OR column_name LIKE '%photo%' OR column_name LIKE '%pic%')" 2>/dev/null)
MISS=0; TOTAL=0
for tc in $COLS; do
  t="${tc%%.*}"; c="${tc#*.}"
  vals=$(docker exec "$DB_CONTAINER" mysql -u"$DB_USER" -p"$DB_PASS" -N -e \
    "SELECT \`$c\` FROM \`$DB_NAME\`.\`$t\` WHERE \`$c\` LIKE '%/files/%'" 2>/dev/null || true)
  while IFS= read -r v; do
    [ -z "$v" ] && continue
    IFS=',' read -ra parts <<< "$v"
    for part in "${parts[@]}"; do
      case "$part" in *"/files/"*) ;; *) continue ;; esac
      fname="${part##*/files/}"
      TOTAL=$((TOTAL+1))
      if ! docker exec "$APP_CONTAINER" test -f "/app/uploads/$fname"; then
        echo "   MISS: $t.$c -> $fname"
        MISS=$((MISS+1))
      fi
    done
  done <<< "$vals"
done
if [ "$MISS" = 0 ]; then
  echo "== 恢复成功：DB 与图片完全对齐（$TOTAL 条引用全部命中）"
else
  echo "== WARN: $MISS/$TOTAL 条引用缺图（应与 manifest 的 missing 一致，属备份时即缺）"
fi
