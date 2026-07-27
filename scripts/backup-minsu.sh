#!/bin/bash
# minsu-seatech 全量备份：MySQL dump + uploads 图片卷 + 对齐 manifest → 单一 bundle。
#
# 设计：
#   - DB dump 与图片快照在同一次运行内完成并打进同一个 tar.gz，恢复时天然对齐
#   - manifest.json 记录「DB 引用的图片 vs 实际存在的图片」差集，恢复后可校验
#   - 变更检测：内容与上一次完全相同则跳过出包（写 heartbeat），避免 OSS 堆冗余
#   - 出包后自动调 sync-minsu-oss.py 异地上传（失败不影响本地备份已落盘）
#
# cron:  25 * * * * /home/steven-amd/github/minsu-seatech/scripts/backup-minsu.sh >> /home/steven-amd/backups/minsu/backup.log 2>&1

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKUP_DIR=/home/steven-amd/backups/minsu
RETENTION_DAYS=7
DB_CONTAINER=minsu-mysql
APP_CONTAINER=minsu-backend
DB_USER=minsu
DB_PASS=minsu123
DB_NAME=minsu

TS=$(date +%Y%m%d_%H%M%S)
STATE_FILE="$BACKUP_DIR/.last_content_hash"
HEARTBEAT="$BACKUP_DIR/.last_run_ok"

log() { echo "[$(date '+%F %T')] $*"; }

WORK=$(mktemp -d "$BACKUP_DIR/.work.XXXXXX")
trap 'rm -rf "$WORK"' EXIT

for c in "$DB_CONTAINER" "$APP_CONTAINER"; do
  if ! docker ps --format '{{.Names}}' | grep -q "^${c}$"; then
    log "FAIL: 容器 $c 未运行，中止备份"
    exit 1
  fi
done

# ── 1. 数据库全量 dump（--skip-dump-date 保证内容不变时字节一致，供变更检测） ──
docker exec "$DB_CONTAINER" mysqldump -u"$DB_USER" -p"$DB_PASS" \
  --single-transaction --skip-dump-date --routines --triggers --events \
  "$DB_NAME" > "$WORK/db.sql" 2>/dev/null

# ── 2. 图片卷快照（紧跟 dump，最小化两者时间差） ──
docker exec "$APP_CONTAINER" tar -C /app -cf - uploads | tar -xf - -C "$WORK"

# ── 3. 生成对齐 manifest：动态扫 information_schema，DB 引用 vs 实际文件 ──
COLS=$(docker exec "$DB_CONTAINER" mysql -u"$DB_USER" -p"$DB_PASS" -N -e \
  "SELECT CONCAT(table_name,'.',column_name) FROM information_schema.columns
   WHERE table_schema='$DB_NAME' AND data_type IN ('varchar','text','mediumtext')
     AND (column_name LIKE '%image%' OR column_name LIKE '%img%'
       OR column_name LIKE '%avatar%' OR column_name LIKE '%url%'
       OR column_name LIKE '%photo%' OR column_name LIKE '%pic%')" 2>/dev/null)

REFS_RAW=""
for tc in $COLS; do
  t="${tc%%.*}"; c="${tc#*.}"
  v=$(docker exec "$DB_CONTAINER" mysql -u"$DB_USER" -p"$DB_PASS" -N -e \
    "SELECT \`$c\` FROM \`$DB_NAME\`.\`$t\` WHERE \`$c\` LIKE '%/files/%'" 2>/dev/null || true)
  [ -n "$v" ] && REFS_RAW+="$v"$'\n'
done

printf '%s' "$REFS_RAW" > "$WORK/.refs"
python3 - "$WORK" "$TS" <<'PYEOF'
import json, os, sys, hashlib
work, ts = sys.argv[1], sys.argv[2]
refs = set()
for line in open(os.path.join(work, '.refs')).read().splitlines():
    for part in line.split(','):          # feedback.images 等逗号分隔多图字段
        part = part.strip()
        if '/files/' in part:
            refs.add(part.split('/files/')[-1])
present = {}
updir = os.path.join(work, 'uploads')
for root, _, files in os.walk(updir):
    for f in files:
        p = os.path.join(root, f)
        rel = os.path.relpath(p, updir)
        h = hashlib.md5(open(p, 'rb').read()).hexdigest()
        present[rel] = {'size': os.path.getsize(p), 'md5': h}
missing = sorted(refs - set(present))     # DB 引用但文件缺失 → 恢复会破图
orphan  = sorted(set(present) - refs)     # 文件存在但 DB 未引用 → 无害
manifest = {
    'backup_time': ts,
    'db_dump': 'db.sql.gz',
    'referenced_count': len(refs),
    'present_count': len(present),
    'missing': missing,
    'orphan': orphan,
    'files': present,
}
with open(os.path.join(work, 'manifest.json'), 'w') as fp:
    json.dump(manifest, fp, ensure_ascii=False, indent=1, sort_keys=True)
if missing:
    print(f"WARN: {len(missing)} 个 DB 引用的图片文件缺失: {missing}")
PYEOF

# ── 4. 变更检测：db.sql + 全部图片内容摘要 ──
CONTENT_HASH=$( { sha256sum "$WORK/db.sql" | awk '{print $1}';
                  (cd "$WORK/uploads" && find . -type f -print0 | sort -z | xargs -0 -r md5sum); } | sha256sum | awk '{print $1}')
if [ -f "$STATE_FILE" ] && [ "$(cat "$STATE_FILE")" = "$CONTENT_HASH" ]; then
  log "SKIP: 内容与上次备份完全一致 (hash=${CONTENT_HASH:0:12}…)，不出新包"
  date +%s > "$HEARTBEAT"
  # touch 最新包：向日报证明「内容仍是最新」，也防止唯一现行备份被 7 天保留期误删
  LATEST_BUNDLE=$(ls -1 "$BACKUP_DIR"/minsu_full_*.tar.gz 2>/dev/null | sort | tail -1 || true)
  [ -n "$LATEST_BUNDLE" ] && touch "$LATEST_BUNDLE"
  exit 0
fi

# ── 5. 出包 ──
gzip -9 "$WORK/db.sql"
BUNDLE="$BACKUP_DIR/minsu_full_${TS}.tar.gz"
tar -C "$WORK" -czf "$BUNDLE" db.sql.gz uploads manifest.json
echo "$CONTENT_HASH" > "$STATE_FILE"
date +%s > "$HEARTBEAT"
log "OK: $BUNDLE ($(ls -lh "$BUNDLE" | awk '{print $5}'))"

# ── 6. 本地保留 ${RETENTION_DAYS} 天 ──
find "$BACKUP_DIR" -maxdepth 1 -name 'minsu_full_*.tar.gz' -mtime +"$RETENTION_DAYS" -delete

# ── 7. 异地上传（OSS 侧不删除，交给 Bucket Lifecycle） ──
if ! "$SCRIPT_DIR/sync-minsu-oss.py" ; then
  log "WARN: OSS 同步失败（本地备份已落盘，下轮会自动补传）"
  exit 1
fi
