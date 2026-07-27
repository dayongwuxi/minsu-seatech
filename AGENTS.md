# 民宿在线预约管理系统项目说明与代码审计报告

> 审计日期：2026-07-24  
> 审计范围：`backend/`、`frontend/`、`backend/sql/`、`scripts/`、`docker-compose.yml`、Cloudflare 配置  
> 审计方式：静态代码审阅、敏感信息与危险调用检索、数据库/实体/Mapper 映射比对、后端及前端测试与构建。当前环境没有 Docker，因此没有执行真实 MySQL + 全栈容器联调。

## 一、结论先行

这是一个功能覆盖较完整的民宿预约系统源码，Java 后台、两个 Vue 前端和 MySQL 初始化脚本均可编译，业务模块基本齐全，具备继续二次开发的代码基础。

但是当前版本**不能直接作为生产系统上线**。审计没有发现混淆代码、远程命令执行、隐藏系统用户或秘密密码校验分支，但发现了数个具有“后门等效效果”的高危问题：

1. 固定 JWT 密钥允许任何拿到源码的人伪造管理员 Token，直接绕过登录。
2. 模拟支付接口在后端始终可调用，可不经过真实支付就把订单改为已支付。
3. 后台只有登录认证，没有真正的角色/接口权限校验；任意后台账号都拥有系统管理、管理员管理、退款等全部接口权限。
4. 初始超级管理员固定为 `admin / admin123`，新增管理员和会员的默认密码为 `123456`。
5. LinkTrust 默认使用明文 HTTP，且渠道查询成功时允许响应不带金额，支付结果可被网络中间人伪造。

因此，项目的准确定位是：

- **代码结构完整度：较高**
- **可二次开发性：可以**
- **当前生产安全性：不合格**
- **建议：先完成“上线阻断项”，再开展业务二开**

## 二、项目用途与技术架构

### 2.1 功能范围

用户端包含：

- 注册、登录、图形验证码、个人资料和密码修改
- 房型/房间浏览、日期筛选、收藏
- 实时报价、优惠活动、下单、取消、退款申请
- Mock、Stripe、LinkTrust 支付
- 入住记录、评价、投诉反馈
- 公告、在线客服
- 9 种语言及多货币展示

管理端包含：

- 数据看板
- 会员类型、会员、房型、房间、周边设施管理
- 订单、入住、收入、促销、退款、汇率管理
- 评价、反馈、公告、客服管理
- 菜单、角色、管理员、系统参数、操作日志管理
- Ollama Cloud 文案生成/翻译

### 2.2 技术栈

| 层级 | 技术 |
|---|---|
| 后端 | Java 17、Spring Boot 3.3.5、MyBatis-Plus 3.5.9 |
| 认证 | JJWT 0.12.6、Bearer Token、BCrypt |
| 数据库 | MySQL 8、utf8mb4、InnoDB |
| 用户端 | Vue 3、Vite、Pinia、Vue Router、Element Plus、Vue I18n |
| 管理端 | Vue 3、Vite、Pinia、Vue Router、Element Plus、ECharts |
| 支付 | Mock、Stripe、LinkTrust Pay |
| 部署 | Docker Compose、Nginx、Cloudflare Tunnel |
| 文件 | 本地/容器卷 `uploads`，通过 `/files/**` 访问 |
| 备份 | MySQL dump + uploads 打包，支持阿里云 OSS 异地同步 |

### 2.3 目录职责

```text
backend/
  src/main/java/com/seatech/minsu/
    controller/          用户端接口
    controller/admin/    管理端接口
    service/             业务服务、支付、退款、定时任务
    mapper/              MyBatis-Plus Mapper 和少量注解 SQL
    entity/              数据库实体
    dto/                 请求与响应对象
    config/              JWT、拦截器、初始化、支付和 LLM 配置
  src/test/              后端单元测试
  sql/schema.sql         最新全量建库脚本
  sql/migrations/        V2～V8 手工增量脚本及数据修复脚本

frontend/user/            用户端 SPA
frontend/admin/           管理端 SPA
frontend/nginx.conf       静态站点和后端反向代理
scripts/                  备份、恢复、健康检查、OSS 同步
cloudflare/               固定域名和 Tunnel 配置
docs/                     设计稿、支付/退款设计、实施记录
docker-compose.yml        MySQL、后端、Web、Cloudflared 编排
```

## 三、隐藏后门与安全审计

### 3.1 审计总判断

没有发现以下典型隐藏后门特征：

- `Runtime.exec`、`ProcessBuilder`、动态脚本引擎等系统命令入口
- 隐藏的万能密码比较分支
- 混淆或 Base64 解码后动态执行的代码
- 未说明的远程代码下载和加载
- SQL 中预埋的隐藏管理员 BCrypt 密文

但下列功能可以产生与后门相同的实际结果，必须按安全漏洞处理。

### 3.2 P0：上线阻断项

#### P0-1 固定 JWT 密钥，可伪造任意管理员身份

- 证据：`backend/src/main/resources/application.yml:30`
- 固定值：`change-me-to-a-random-256bit-secret-change-me-please-0001`
- `JwtUtil` 的管理员 Token 只包含数字用户 ID 和 `aud=admin`。
- `AuthInterceptor` 只验证签名、过期时间和 audience，不查询管理员是否存在、是否禁用、角色是否有效。

攻击者已知源码后可以自行签发 `aud=admin` 的 Token，以管理员 ID 调用所有 `/api/admin/**` 接口。这是直接的认证绕过。

整改要求：

- JWT 密钥必须从环境变量/Secret Manager 注入，生产环境缺失时启动失败，禁止提供默认值。
- 使用至少 32 字节的随机密钥，现有环境必须立即轮换。
- Token 增加 `jti` 或 `tokenVersion`，支持密码修改、禁用账号和退出后失效。
- 每次管理端请求至少校验管理员存在且 `status=1`。

#### P0-2 模拟支付接口可绕过真实付款

- 证据：`backend/src/main/java/com/seatech/minsu/controller/PaymentController.java:91-103`
- `POST /api/payments` 不检查 Stripe/LinkTrust 是否关闭，也没有检查 `dev` Profile。
- 它无条件调用 `MockPayChannel.createPayment`。
- `MockPayChannel` 直接写入 `payment.pay_status=1` 并把 `booking.pay_status=1`。

前端是否显示“模拟支付”不构成安全控制。即使生产前端隐藏按钮，登录用户仍可直接调用接口免费完成订单。

整改要求：

- 增加显式配置 `PAYMENT_MOCK_ENABLED=false`，生产默认关闭。
- Mock Controller/Bean 只允许在 `dev`、`test` Profile 注册。
- 真实支付渠道启用时，后端必须拒绝 Mock 请求。
- 增加接口测试，验证生产 Profile 下 `/api/payments` 返回拒绝。

#### P0-3 后台 RBAC 未真正生效

- `role`、`menu`、`role_menu` 表虽然存在，但只用于后台配置页面。
- 全仓库未发现 `@PreAuthorize`、`hasRole`、权限注解、接口权限拦截器或等价逻辑。
- `WebConfig` 只区分会员 Token 与管理员 Token。
- 前端侧边栏也是固定菜单，没有按角色过滤。

结果是客服、财务、运营等任何管理员都能：

- 新建管理员、修改管理员角色和密码
- 修改角色和菜单
- 清空操作日志
- 操作订单、退款、收入和系统配置

整改要求：

- 采用 Spring Security 或自定义权限拦截器，在后端按权限码保护每个管理接口。
- 菜单显示权限与接口操作权限分开建模，不能只依赖 `role_menu`。
- 管理员/角色/日志清理等接口只允许 `super_admin`。
- 退款、收入等资金接口应使用独立权限，并记录不可清除的审计日志。

#### P0-4 LinkTrust 支付链路使用明文 HTTP

- 证据：`backend/src/main/java/com/seatech/minsu/config/LinkTrustConfig.java:37-38`
- 收银台和服务端交易反查默认地址均为 `http://pay.linktrust-pay.com/...`。
- IPN 本身没有签名，代码依赖服务端反查确认。
- `PaymentSyncService.applyLinkTrustTxn` 只在渠道金额“存在且不相等”时拒绝；成功响应不带金额仍会落账。

明文 HTTP 使反查响应可被网络中间人篡改。伪造 `result=success` 且省略 `amount`，当前代码仍可能把订单标为已支付。

整改要求：

- 供应商不能提供 HTTPS 或可靠签名时，不应在公网生产环境使用该渠道。
- 成功响应必须同时满足：订单号、商户号、币种、金额、交易号、支付方式均存在且匹配。
- 缺失金额必须拒绝，不能当作可选字段。
- 给交易反查增加证书校验、重放防护、完整审计与异常告警。

### 3.3 P1：高优先级风险

#### 固定高权限账号与弱默认密码

| 对象 | 默认值 | 位置 |
|---|---|---|
| 初始超级管理员 | `admin / admin123` | `DataInitializer.java`、`README.md` |
| 新增管理员默认密码 | `123456` | `AdminSystemController.java:48` |
| 后台新增会员默认密码 | `123456` | `AdminMemberController.java:40` |
| MySQL root | `root123` | `docker-compose.yml` |
| MySQL 应用用户 | `minsu / minsu123` | `docker-compose.yml`、`application-dev.yml`、备份/恢复脚本 |

`DataInitializer` 在管理员表为空时自动创建 `role_id=1` 的管理员。虽然行为写在注释和 README 中，不属于“隐藏”账号，但它是公开的最高权限入口。

建议删除固定口令：首次启动从一次性环境变量创建管理员，或提供只在本机执行的初始化命令；首次登录必须强制改密。

此外，`application.yml` 把 `spring.profiles.active` 固定为 `dev`，仓库也没有独立的生产配置文件。正式部署必须显式设置生产 Profile，并让数据库、JWT、支付和外联密钥在缺失时启动失败。

#### 支付状态更新不具备原子性

`PaymentStateService.markSucceeded` 没有事务：

1. 先更新 payment 为成功；
2. 再更新 booking 为已支付。

如果第二步失败，重试时会因为 payment 已是成功状态而直接返回，booking 可能永久保持未支付。`MockPayChannel` 也存在类似问题。订单取消后创建退款申请、评价后更新入住记录等流程也有跨表非事务写入。

建议把支付、订单、退款、入住等状态机放入事务 Service，使用条件更新/乐观锁保证幂等，不在 Controller 中拼接跨表事务。

#### 公告存在存储型 XSS

- 证据：`frontend/user/src/views/NoticeDetail.vue:11`
- 后台可写入公告 HTML，用户端用 `v-html` 直接渲染，未发现 HTML 清洗。
- Token 保存在 `localStorage`，XSS 成功后可读取用户 Token。

建议后端入库时使用白名单 HTML Sanitizer，前端渲染前再次清洗，并设置严格 CSP。

#### 自动备份存在数据库出站传输

`scripts/backup-minsu.sh` 会把完整数据库和 uploads 打包后自动调用 `sync-minsu-oss.py`。目标 Bucket、Endpoint 和密钥来自宿主机 `.oss.env`；`restore-minsu.sh` 注释还保留了 `steven-china-storage` 的历史目标。

这不是隐藏上传逻辑，源码注释明确说明了用途，但接手项目时必须：

- 审核并替换 `.oss.env`，确认 Bucket 归属和数据驻留要求。
- 未获批准前不要安装对应 cron。
- 对备份加密后再上传，密钥与 OSS 凭据分离。
- 审核 ntfy 告警目标和 Cloudflare Tunnel 所属账号。

#### 已禁用账号的旧 Token 不会立即失效

登录时会检查账号状态，后续请求只验证 JWT。管理员或会员被禁用后，已签发 Token 最长仍可使用 24 小时；退出接口也只返回成功，不做服务端失效。

#### 订单号仅有 4 位随机数

`NoGenerator` 使用“日期 + 0000～9999 随机数”，数据库虽有唯一索引，但业务代码没有捕获冲突并重试。并发量上升后会出现随机插入失败，不适合长期扩展。建议改为数据库序列、Snowflake、ULID 或更长的安全随机 ID。

### 3.4 P2：需要在正式二开中处理

- CORS 使用 `allowedOriginPatterns("*")`，应收敛到正式域名。
- 登录没有限流、失败次数锁定、IP/账号风控。
- 后端注册和改密没有统一强密码策略，DTO 基本未使用 Bean Validation。
- JWT 保存在 `localStorage`，需结合 CSP、XSS 清洗或改用安全 Cookie 方案。
- 静态文件 `/files/**` 公开访问，没有鉴权和病毒扫描。
- 上传仅依赖 MIME/扩展名及图片重编码，仍需限制像素、解码资源消耗和恶意图片。
- 管理端可清空操作日志，不满足强审计要求。
- npm 安装报告：用户端 3 个依赖漏洞（1 moderate、2 high）；管理端 6 个（4 moderate、1 high、1 critical）。升级前应运行 `npm audit` 核对具体依赖和兼容性。

## 四、Java 后台与数据库完整性

### 4.1 静态映射结果

自动比对结果：

| 项目 | 数量 | 结果 |
|---|---:|---|
| Java 主代码文件 | 264 | Controller/Service/Mapper/Entity/DTO/配置齐全 |
| Controller | 38 | 用户端与管理端接口均存在 |
| 数据表 | 28 | `schema.sql` 可找到完整定义 |
| Entity | 28 | 与 28 张表一一对应 |
| Mapper | 28 | 与 28 个 Entity 一一对应 |
| 后端测试类 | 28 | 重点覆盖支付、退款、价格、预约、文件和 LLM |

字段级比对中，Entity 字段均能在 `schema.sql` 找到。唯一的数据库额外字段是：

- `payment.pending_flag`：MySQL 虚拟生成列，用于限制同订单、同渠道最多一条待支付流水；不映射到 Entity 是正常设计。

### 4.2 构建与测试结果

本次实际执行结果：

- `mvn test`：**161 tests，0 failures，0 errors**
- 用户端 `npm test`：**95 tests，全部通过**
- 管理端 `npm test`：**37 tests，全部通过**
- 用户端 `npm run build`：成功
- 管理端 `npm run build`：成功

用户端测试结束时出现对 `localhost:3000` 的 `ECONNREFUSED` 日志，但 Vitest 最终为通过；建议后续清理未正确 Mock 的网络请求，避免测试假阳性。

### 4.3 数据库设计的优点

- 全表统一使用 MySQL 8、InnoDB、utf8mb4。
- 核心业务有必要的唯一索引和查询索引。
- 订单价格由后端重算，避免直接信任前端金额。
- 房源下单使用 `SELECT ... FOR UPDATE` 防止并发重复预订。
- 支付待处理流水使用虚拟列 + 唯一索引作为并发闸门。
- 促销名额使用条件原子更新。
- `schema.sql` 已包含 V2～V8 的最终结构，新环境不需要逐个执行迁移。
- 备份脚本同时备份数据库和图片，并提供 manifest 对齐校验。

### 4.4 数据库完整性的缺口

#### 没有外键和 CHECK 约束

表中大量字段注释为 FK，但 `schema.sql` 没有实际的 `FOREIGN KEY`：

- `admin.role_id`
- `booking.member_id/room_id/promotion_id`
- `payment.booking_id/member_id`
- `refund_record.booking_id/member_id/handler_id`
- `review.member_id/room_id/booking_id`
- `room_image.room_id`
- `role_menu.role_id/menu_id`
- 客服、收藏、入住等关联字段

删除或异常写入可能产生孤儿数据；状态值、金额、日期区间也没有数据库级 CHECK。二开时应确定删除策略，再补充外键、检查约束或定期一致性校验。

#### 迁移依赖人工执行

仓库有 `migrate-v2` 到 `migrate-v8`，但没有 Flyway/Liquibase：

- 无法自动判断某环境当前版本。
- 迁移脚本不保证全部幂等。
- 容器的 `/docker-entrypoint-initdb.d` 只在空数据卷首次启动时执行。
- 已存在数据库仅更新代码不会自动更新表结构。

建议引入 Flyway，建立 `V1__baseline.sql` 和后续版本，并在 CI 中测试“空库升级”和“旧版本升级”。

#### 缺少真实数据库集成测试

当前后端测试主要使用 Mockito，不会验证：

- MyBatis 注解 SQL 在 MySQL 8 上能否全部执行
- 逻辑删除条件是否符合预期
- 虚拟列及唯一索引的真实并发行为
- 全量 `schema.sql` 和 V2～V8 增量升级是否等价
- 时区、字符集、金额精度和锁等待行为

建议使用 Testcontainers MySQL 8 添加集成测试。当前环境没有 Docker，所以本次无法完成该项实测。

#### 金额币种迁移有历史混算风险

V6 把站点基准从 CNY 改为 JPY，但迁移注释明确说明历史 booking/payment 保留原币种。收入统计如果直接求和，会混合 CNY 与 JPY。二开前必须确定历史数据换算、分币种统计和财务对账方案。

## 五、是否适合二次开发

### 5.1 可以继续开发的理由

- 分层清晰，Controller、Service、Mapper、Entity、DTO 基本齐全。
- 用户端和管理端功能覆盖完整，不是只有页面的空壳项目。
- 核心支付、价格、退款、房态并发已有专门 Service 和测试。
- 全量 schema 与当前实体基本同步。
- 两个前端和后端均能构建，测试基线可用。
- 支付渠道通过 `PayChannel` 抽象，后续可增加新渠道。
- 房间国际化、汇率、LLM 等扩展点已经拆分。

### 5.2 不建议直接上线的理由

- 认证可被固定 JWT 密钥绕过。
- Mock 支付可绕过真实付款。
- 后台角色权限只停留在数据表和页面，未形成安全边界。
- LinkTrust 明文链路不满足支付安全要求。
- 缺少数据库版本管理和真实 MySQL 集成测试。
- 资金状态跨表更新存在事务一致性缺口。

### 5.3 建议的二开顺序

1. **安全封堵**：轮换 JWT、删除默认密码、生产禁用 Mock、停用不安全 LinkTrust。
2. **权限重建**：接入 Spring Security，完成管理员 RBAC 和接口权限测试。
3. **状态机事务化**：统一订单、支付、退款、入住的事务和幂等策略。
4. **数据库工程化**：引入 Flyway、外键/一致性检查、Testcontainers。
5. **前端安全**：公告 HTML 清洗、CSP、Token 安全、CORS 白名单。
6. **部署脱敏**：数据库密码、Tunnel、OSS、ntfy、域名全部改为环境配置。
7. **业务二开**：在安全基线之上增加新房态、定价、支付或运营功能。

完成前四项后，项目可作为常规业务系统持续二开；未完成前不应接入真实资金和真实客户数据。

## 六、开发、测试与部署

### 6.1 后端

要求 JDK 17+、Maven 3.9+、MySQL 8。

```bash
cd backend
mvn test
mvn spring-boot:run
```

默认开发库连接见 `backend/src/main/resources/application-dev.yml`。正式环境必须覆盖数据库密码和 JWT 密钥，不能使用仓库默认值。

### 6.2 前端

```bash
cd frontend/user
npm ci
npm test
npm run dev

cd ../admin
npm ci
npm test
npm run dev
```

生产构建：

```bash
cd frontend/user && npm run build
cd frontend/admin && npm run build
```

### 6.3 Docker Compose

```bash
docker compose up -d --build
```

当前 Compose 会同时启动 MySQL、后端、Nginx 和 Cloudflare Tunnel。接手环境前必须先修改：

- MySQL root 与应用账号密码
- JWT 密钥注入
- Cloudflare Tunnel ID、凭据路径和域名
- Stripe/LinkTrust 配置
- Ollama API Key
- OSS 备份目标与凭据

### 6.4 数据库更新规则

- 新环境：执行最新 `backend/sql/schema.sql`。
- 旧环境：当前只能按版本顺序人工执行 `backend/sql/migrations/`。
- 引入 Flyway 后，以 Flyway 历史表为唯一版本依据，不再手工修改生产表。
- 任何实体字段变更必须同时提供迁移、回滚/兼容说明和 MySQL 集成测试。

## 七、接手与提交前检查清单

- [ ] 生产 JWT 密钥已随机生成并从外部注入
- [ ] `admin/admin123` 和所有 `123456` 默认密码入口已移除
- [ ] 生产 Profile 无法装配或调用 Mock 支付
- [ ] 管理端每个敏感接口都有后端权限校验
- [ ] LinkTrust 已改为可信 HTTPS/签名方案，否则已停用
- [ ] 公告 HTML 已做服务端白名单清洗
- [ ] 支付/订单/退款跨表操作有事务和幂等测试
- [ ] Flyway 和 MySQL 集成测试已接入 CI
- [ ] CORS 仅允许正式域名
- [ ] 数据库、OSS、Cloudflare、ntfy 均已替换为本方账号
- [ ] 备份已加密并完成恢复演练
- [ ] `npm audit` 与 Maven 依赖漏洞已处理或形成风险接受记录
- [ ] 当前源码目录已纳入有效 Git 仓库；本次审计环境中 `git status` 未识别到仓库元数据

## 八、维护约定

- 安全控制必须在后端实施，前端隐藏按钮不等于权限控制。
- 禁止在源码、Compose、SQL、脚本中新增真实账号、密码、Token、商户号和云凭据。
- 禁止让测试/Mock 支付在生产 Profile 中可用。
- 所有资金状态变更必须由 Service 事务完成，并可安全重试。
- 所有用户归属资源必须校验当前用户 ID，管理端资源必须校验权限码。
- 新增表或字段必须同步 Entity、Mapper、全量 schema、版本迁移和集成测试。
- 富文本、链接和上传文件都按不可信输入处理。
- 操作日志和财务日志应不可由普通管理员清空或篡改。
