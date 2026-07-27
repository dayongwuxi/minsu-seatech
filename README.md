# 民宿在线预约管理系统 (minsu-seatech)

依据《民宿在线预约管理系统基本设计》（`docs/design-mockups/`，19 页 PDF + 32 张画面图）落地。

**技术栈**：Vue3（前端） · Java 17 + Spring Boot 3.3（后端接口，JSON） · MySQL 8（数据库） · MyBatis-Plus · Element Plus · ECharts

## 目录结构

```
minsu-seatech
├── backend/                 # Spring Boot 后端 (controller/service/mapper/entity/dto)
│   └── sql/schema.sql       # 建库建表 + 种子数据
├── frontend/
│   ├── user/                # 用户端 Vue3 (端口 5173)
│   └── admin/               # 管理端 Vue3 (端口 5174)
├── docs/
│   ├── design-mockups/      # 原始设计图与基本设计书
│   ├── 审阅纪要.md           # 32张画面逐张审阅结论、模型与枚举裁定
│   └── 实施计划.md           # 分阶段实施计划 (Phase 0~6)
└── docker-compose.yml       # 开发用 MySQL 8
```

## 线上站点（全容器化 + 独立 Cloudflare 隧道）

- 用户端：**https://seabnb.axionintell.com/**
- 管理端：**https://seabnb.axionintell.com/admin/**

```bash
docker compose up -d --build   # 一条命令拉起全站 4 个容器
```

| 容器 | 作用 | 宿主机端口 |
|------|------|-----------|
| minsu-mysql | MySQL 8（首启自动执行 backend/sql/schema.sql） | 13306 |
| minsu-backend | Spring Boot 接口（镜像内 Maven+JDK17 多阶段构建，本机无需 JDK） | 8090 |
| minsu-web | nginx：`/` 用户端、`/admin` 管理端、`/api` `/files` 反代后端 | 28080（本机调试） |
| minsu-cloudflared | 本项目**专属独立隧道** minsu-seabnb（id 14085a84…），公网流量走 QUIC 出站，不占入站端口 | — |

> 端口说明：本机已有多个项目占用 8080/5173/3306 等常用端口，故本项目统一使用
> MySQL **13306** / 后端 **8090** / web **28080** / 前端 dev **25173/25174**，避免冲突。
> 隧道凭证在 `~/.cloudflared/14085a84-e44c-4901-a3d3-e3102eb22772.json`（0400，勿入库）；
> 隧道路由配置在 `cloudflare/config.yml`。

### 发布更新

```bash
# 前端（用户端/管理端）或 nginx 配置改动：
docker compose up -d --build web
# 后端改动：
docker compose up -d --build backend
```

### 本地开发（热更新，可选）

```bash
cd frontend/user  && npm install && npm run dev   # http://localhost:25173
cd frontend/admin && npm install && npm run dev   # http://localhost:25174
```
注意：管理端生产构建挂在 `/admin/` 子路径（vite `base` + router `BASE_URL` 已配置），本地 dev 仍在根路径运行。

初始管理员：`admin / admin123`（后端首次启动自动创建，登录后请立即修改）。

## 约定

- 接口统一返回 `{ code, message, data }`，`code=0` 成功；分页返回 `{ total, pages, current, size, records }`
- 认证：JWT Bearer Token，用户端与管理端分离（`/api/**` 会员 token、`/api/admin/**` 管理员 token）
- 状态枚举与单号规则见 `docs/审阅纪要.md` 第 4 节，各端必须一致
