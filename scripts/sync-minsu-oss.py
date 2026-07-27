#!/home/steven-amd/backups/minsu/.venv/bin/python
"""把 /home/steven-amd/backups/minsu/ 下的 minsu_full_*.tar.gz 同步到阿里云 OSS。

Key 布局:
  db-backup/seatech-minsu/<YYYY>/<MM>/<DD>/minsu_full_YYYYMMDD_HHMMSS.tar.gz

设计(对齐 ai-trade sync_backups_to_oss.py 的既有约定):
  - 幂等: HeadObject 判已存在则 skip,重跑/补传安全
  - 校验: 上传后 HeadObject 重读 size 与本地一致才算成功
  - 不删除: OSS 侧保留交给 Bucket Lifecycle,本脚本不需要 Delete 权限
  - 凭证: /home/steven-amd/backups/minsu/.oss.env (chmod 600)

用法:
  sync-minsu-oss.py                  # 同步所有未上传的 bundle
  sync-minsu-oss.py --dry-run        # 只打印不真传
  sync-minsu-oss.py --verify-latest  # 只校验最新本地 bundle 已在 OSS(healthcheck 用)

退出码: 0 全部成功(含 skip) / 1 有失败 / 2 配置错误
"""

import argparse
import re
import sys
from pathlib import Path

BACKUP_DIR = Path("/home/steven-amd/backups/minsu")
ENV_FILE = BACKUP_DIR / ".oss.env"
BUNDLE_RE = re.compile(r"^minsu_full_(\d{4})(\d{2})(\d{2})_\d{6}\.tar\.gz$")


def load_env(path: Path) -> dict:
    conf = {}
    if not path.is_file():
        print(f"[sync] ERROR: 缺少凭证文件 {path}", file=sys.stderr)
        sys.exit(2)
    for line in path.read_text().splitlines():
        line = line.strip()
        if line and not line.startswith("#") and "=" in line:
            k, _, v = line.partition("=")
            conf[k.strip()] = v.strip()
    for k in ("OSS_BACKUP_ENDPOINT", "OSS_BACKUP_BUCKET",
              "OSS_BACKUP_ACCESS_KEY_ID", "OSS_BACKUP_ACCESS_KEY_SECRET"):
        if not conf.get(k):
            print(f"[sync] ERROR: {path} 缺少 {k}", file=sys.stderr)
            sys.exit(2)
    conf.setdefault("OSS_BACKUP_KEY_PREFIX", "db-backup/seatech-minsu")
    return conf


def make_bucket(conf: dict):
    import oss2
    auth = oss2.Auth(conf["OSS_BACKUP_ACCESS_KEY_ID"],
                     conf["OSS_BACKUP_ACCESS_KEY_SECRET"])
    endpoint = conf["OSS_BACKUP_ENDPOINT"]
    if not endpoint.startswith("http"):
        endpoint = "https://" + endpoint
    return oss2.Bucket(auth, endpoint, conf["OSS_BACKUP_BUCKET"])


def key_for(name: str, prefix: str) -> str:
    m = BUNDLE_RE.match(name)
    yyyy, mm, dd = m.group(1), m.group(2), m.group(3)
    return f"{prefix}/{yyyy}/{mm}/{dd}/{name}"


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--dry-run", action="store_true")
    ap.add_argument("--verify-latest", action="store_true")
    args = ap.parse_args()

    conf = load_env(ENV_FILE)
    prefix = conf["OSS_BACKUP_KEY_PREFIX"].strip("/")
    bundles = sorted(p for p in BACKUP_DIR.glob("minsu_full_*.tar.gz")
                     if BUNDLE_RE.match(p.name))
    if not bundles:
        print("[sync] 本地无 bundle,无事可做")
        return 0

    import oss2
    bucket = make_bucket(conf)

    if args.verify_latest:
        latest = bundles[-1]
        key = key_for(latest.name, prefix)
        try:
            meta = bucket.head_object(key)
        except oss2.exceptions.NoSuchKey:
            print(f"[sync] VERIFY FAIL: 最新 bundle 未上传 OSS: {key}")
            return 1
        if meta.content_length != latest.stat().st_size:
            print(f"[sync] VERIFY FAIL: OSS size {meta.content_length} != 本地 {latest.stat().st_size}: {key}")
            return 1
        print(f"[sync] VERIFY OK: {key} ({meta.content_length} bytes)")
        return 0

    failed = 0
    for p in bundles:
        key = key_for(p.name, prefix)
        size = p.stat().st_size
        try:
            meta = bucket.head_object(key)
            if meta.content_length == size:
                continue  # 已上传且一致
            print(f"[sync] WARN: {key} 已存在但 size 不一致,重传")
        except oss2.exceptions.NoSuchKey:
            pass
        if args.dry_run:
            print(f"[sync] DRY-RUN would upload {p.name} -> oss://{conf['OSS_BACKUP_BUCKET']}/{key}")
            continue
        try:
            bucket.put_object_from_file(key, str(p))
            meta = bucket.head_object(key)
            if meta.content_length != size:
                raise RuntimeError(f"上传后校验 size 不一致 {meta.content_length}!={size}")
            print(f"[sync] OK {p.name} -> oss://{conf['OSS_BACKUP_BUCKET']}/{key} ({size} bytes)")
        except Exception as e:  # noqa: BLE001 — 单文件失败不阻塞其它文件
            print(f"[sync] FAIL {p.name}: {e}", file=sys.stderr)
            failed += 1
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
