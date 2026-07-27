#!/bin/bash
# minsu 备份健康自检：新鲜度 + bundle 完整性 + 对齐 + OSS 异地副本。
# cron:  40 * * * * /home/steven-amd/github/minsu-seatech/scripts/backup-minsu-healthcheck.sh >> /home/steven-amd/backups/minsu/health.log 2>&1

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKUP_DIR=/home/steven-amd/backups/minsu
HEARTBEAT="$BACKUP_DIR/.last_run_ok"
MAX_AGE_MIN=75   # 小时级备份，允许 75 分钟窗口

FAIL=0
FAIL_MSGS=""
say() { echo "[$(date '+%F %T')] $*"; }
failure() { say "FAIL: $*"; FAIL_MSGS+="$*"$'\n'; FAIL=1; }

# FAIL 时即时 ntfy 告警（复用本机日报同一 topic 配置 /etc/backup-daily-report.env）
send_ntfy_alert() {
  local env_file=/etc/backup-daily-report.env NTFY_URL="" NTFY_TOPIC="" NTFY_TOKEN=""
  [ -r "$env_file" ] || return 0
  # shellcheck disable=SC1090
  . "$env_file"
  [ -n "$NTFY_TOPIC" ] || return 0
  local auth=()
  [ -n "${NTFY_TOKEN:-}" ] && auth=(-H "Authorization: Bearer ${NTFY_TOKEN}")
  curl -sS --max-time 20 -o /dev/null \
    -H "Title: minsu 备份健康自检 FAIL" -H "Priority: high" -H "Tags: floppy_disk,rotating_light" \
    "${auth[@]}" -d "$1" "${NTFY_URL:-https://ntfy.sh}/${NTFY_TOPIC}" 2>/dev/null
}

# 1. 新鲜度：备份脚本 75 分钟内成功跑过（出包或内容未变 skip 都算）
if [ ! -f "$HEARTBEAT" ]; then
  failure "无 heartbeat，备份从未成功运行"
else
  age_min=$(( ( $(date +%s) - $(cat "$HEARTBEAT") ) / 60 ))
  if [ "$age_min" -gt "$MAX_AGE_MIN" ]; then
    failure "备份已 ${age_min} 分钟未成功运行 (>${MAX_AGE_MIN})"
  fi
fi

# 2. 最新 bundle 完整性 + manifest 对齐
LATEST=$(ls -1 "$BACKUP_DIR"/minsu_full_*.tar.gz 2>/dev/null | sort | tail -1 || true)
if [ -z "$LATEST" ]; then
  failure "本地没有任何 bundle"
else
  if ! tar -tzf "$LATEST" >/dev/null 2>&1; then
    failure "bundle 损坏 $LATEST"
  elif ! "$SCRIPT_DIR/restore-minsu.sh" --check "$LATEST" >/dev/null 2>&1; then
    failure "bundle 对齐校验不过 $LATEST（详见 restore-minsu.sh --check）"
  fi
fi

# 3. 异地副本：最新 bundle 必须已在 OSS 且 size 一致
if [ -n "$LATEST" ]; then
  if ! "$SCRIPT_DIR/sync-minsu-oss.py" --verify-latest >/dev/null 2>&1; then
    say "最新 bundle 未上传 OSS 或 size 不一致，尝试补传..."
    "$SCRIPT_DIR/sync-minsu-oss.py" >/dev/null 2>&1 && say "  补传成功" || failure "OSS 补传仍失败"
  fi
fi

if [ "$FAIL" = 0 ]; then
  say "OK: 本地新鲜 + bundle 完整 + 对齐 + OSS 副本一致"
else
  send_ntfy_alert "$FAIL_MSGS"
fi
exit "$FAIL"
