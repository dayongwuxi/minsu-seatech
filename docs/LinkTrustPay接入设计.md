# LinkTrustPay 支付渠道接入设计

> 2026-07-24 设计定稿。依据供应商《LinkTrust Pay 初期設定マニュアル（HTMLリンク接続）》与官方 sample（SysPayService.java），
> 在现有 PayChannel 抽象（见 docs/支付促销退款设计.md）上新增第三方渠道 channel=3。

## 0. 供应商接口规约（事实摘要）

- **接入方式**：HTML リンク（表单跳转托管收银台）。商户侧不接触任何卡/账户敏感信息。
- **发起支付**：HTML FORM **POST** → `http://pay.linktrust-pay.com/webpay/payform`
  - 参数：`merId`（店铺ID，签约发行，如 M810000022）、`merOrderId`（商户订单号，**唯一必须**，回调原样返回）、`amount`（**日元整数**，含税总额）
  - 用户在 LinkTrust 托管页选择 wechatpay / alipay / unionpay 完成支付
- **结果通知（IPN Callback）**：POST 到商户后台配置的「決済結果通知URL」
  - 字段：`transactionId`（LTP 交易ID）、`merId`、`result`（`not found`/`success`/`failure`/`pending`/`error`）、`merOrderId`、`payMethod`（`wechatpay`/`alipay`/`unionpay`）、`amount`
  - 商户未返回 `200 OK` 时**每小时重发**
  - **规约未定义签名机制** → IPN 内容不可信，必须反查确认（见 3.2）
- **主动查询**：POST `http://pay.linktrust-pay.com/webpay/transactions`，请求为**表单编码** `merId=..&merOrderId=..`（2026-07-24 实测：JSON 体被 400 拒绝，PDF 的 JSON Request 示例不可用，以 vendor sample code 的 form 提交为准）→ 响应 JSON `{"transactionId","result","payMethod","amount"}`
- **仅 HTTP 可靠**：渠道 443 虽有证书（FujiSSL DV）但未下发中间证书链（2026-07-24 实测 `unable to verify the first certificate`），严格客户端（Firefox/安卓 Chrome/curl）会证书报错，故 payform/transactions 走 http。
- **跳转方式用 GET 链接而非表单 POST**：HTTPS 页面向 HTTP 目标提交表单会触发浏览器「提交的信息不安全」拦截弹窗；GET 带参普通跳转无此拦截（官方 sample code 同款用法，实测渠道对 GET 直接创建交易并跳转收银台）。若供应商修复证书链，`LINKTRUST_PAYFORM_URL` 换 https 即彻底干净
- **无浏览器回跳 return_url 机制**（规约未提供）→ 用户支付后停留在 LinkTrust 页面，商户站需自行轮询确认
- **退款**：无 API。商户在 LinkTrust 后台人工操作。
- **不支持 credit card**（信用卡结算供应商另行洽谈中）。

## 1. 架构：渠道抽象扩展与路由

```
                     ┌ MockPayChannel      channel=1  RefundMode.INSTANT
PayChannel(接口) ────┼ StripePayChannel    channel=2  RefundMode.API_ASYNC
                     └ LinkTrustPayChannel channel=3  RefundMode.MANUAL_OFFLINE   ← 本次新增
                        （未来：信用卡供应商 channel=4，按同样方式挂接）
```

- `PayChannel` 接口新增：
  - `enum RefundMode { INSTANT, API_ASYNC, MANUAL_OFFLINE }` + `default RefundMode refundMode()`（默认 INSTANT）
    - INSTANT：同意退款即置已退款（Mock）
    - API_ASYNC：调渠道退款 API，webhook 确认到账（Stripe）
    - MANUAL_OFFLINE：**渠道无退款 API**，同意后进入「退款中」，管理员去渠道后台手工退款，回站内点「确认已退款」（LinkTrust）
- 新增 `PayChannelRegistry`：收集全部 `PayChannel` bean，按 channel 编码路由。`RefundService` 等不再硬编码 if(channel==2)。
- **前端路由**：`GET /api/payments/config` 返回各渠道启用状态与支持的支付方式，用户在支付页选择具体「渠道+方式」组合；后端按端点分渠道（`/api/payments`(mock)、`/api/payments/stripe/*`、`/api/payments/linktrust/*`），新渠道以同构方式增加端点即可。
- 支付方式编码扩展：`pay_method` 新增 **7=银联(UnionPay)**（1微信 2支付宝 3银行卡 4其他/聚合 5PayPay预留 6PayPal预留 7银联）。
  LinkTrust 方式映射：wechatpay→1、alipay→2、unionpay→7。

## 2. 配置（环境变量注入，缺省即禁用，与 Stripe 同构）

| 变量 | 说明 |
|---|---|
| `LINKTRUST_MER_ID` | 店铺ID（签约发行）。**留空 = LinkTrust 渠道禁用** |
| `LINKTRUST_PAYFORM_URL` | 收银台地址，默认 `http://pay.linktrust-pay.com/webpay/payform`（渠道仅开 http） |
| `LINKTRUST_QUERY_URL` | 交易查询地址，默认 `http://pay.linktrust-pay.com/webpay/transactions` |
| `LINKTRUST_PAYMENT_METHODS` | 逗号分隔，默认 `wechatpay,alipay,unionpay` |

- `LinkTrustConfig`（仿 StripeConfig）：`@Value` 构造注入，`isEnabled()=merId非空`；`toChannelAmount(BigDecimal)` 金额四舍五入取整（LinkTrust 只收日元整数）。
- **货币前提**：LinkTrust 金额单位为日元。启用该渠道时站点结算货币（`PAYMENT_CURRENCY`，历史名 `STRIPE_CURRENCY` 兜底兼容）应为 `jpy`；若不是，启动时打 warn 日志提醒（金额将按数值取整直传，可能造成币种错配）。

## 3. 支付流程

### 3.1 发起支付
1. 用户支付页选择 微信/支付宝/银联（LinkTrust 渠道）
2. `POST /api/payments/linktrust/create {orderNo, payMethod}`（登录态、校验订单归属与待支付状态）
   - `LinkTrustPayChannel.createPayment()`：复用该订单未完成的 channel=3 支付行（幂等），否则新建
     `payment{payNo=SR…, channel=3, payStatus=0, payMethod, amount=totalAmount, currency}`
   - **merOrderId = payment.payNo**（每次支付尝试唯一，回调按此定位；不用 orderNo，避免重试语义混淆）
   - 返回 `{payUrl, merId, merOrderId, amount}`
3. 前端以 **GET 带参链接新标签页** 打开 LinkTrust 收银台（点击时同步 window.open 防弹窗拦截、被拦截则当前页跳转兜底），同时本页跳 `/pay-result/{orderNo}` 轮询
4. 结果页轮询 `GET /api/payments/result`；超过轮询上限仍待支付时显示「支付确认中，完成支付后点击刷新」态（不误报失败）

### 3.2 IPN 回调（核心安全设计）
`POST /api/payments/linktrust/callback`（表单编码，免登录放行）：

| 步骤 | 判定 | 响应 |
|---|---|---|
| 1 | 渠道未启用 | 503（配置好前让渠道重试） |
| 2 | `merId` ≠ 配置值 | 200 `ignored`（非本店通知，停止重发） |
| 3 | 按 `merOrderId`(=payNo, channel=3) 找不到 payment | 200 `ignored` |
| 4 | **服务端反查** `/webpay/transactions`（IPN 无签名，一律以反查结果为准，忽略 IPN 自带 result） | 反查网络失败 → 500（让渠道一小时后重发） |
| 5 | 反查 `amount` ≠ `toChannelAmount(payment.amount)` | 金额不符：log error 不改状态，200（人工排查，避免每小时无效重发） |
| 6 | 反查 `result=success` → 校正 payMethod 后 `PaymentStateService.markSucceeded(payment, transactionId)`；`failure/error` → `markFailed`；`pending/not found` → 不动 | 200 `success` |

- 幂等：`markSucceeded` 已有已支付短路；重复 IPN 无副作用。
- 订单已被超时取消(booking_status=4)后收到支付成功：仍记支付成功（钱确实收了），log warn 提示人工退款处理。

### 3.3 结果同步兜底
抽取 `PaymentSyncService.sync(payment)`：按渠道查询状态并落库（Stripe 查 PaymentIntent；LinkTrust 反查 transactions）。三个调用方共用：
- `GET /api/payments/result` 结果页兜底（原 PaymentController 内联 Stripe 逻辑迁入）
- 待支付超时释放 Job（取消前最后同步一次，见 5）
- IPN 回调（本质即 sync 语义）

## 4. 退款：人工审核 + 线下操作（需求2）

状态机在原有基础上扩展（refund_record.status 取值不变）：

```
用户申请退款 → status=0 申请中（channel 自动记 3）
管理端「同意」：
   INSTANT(Mock)          → 直接 status=3 已退款
   API_ASYNC(Stripe)      → 调渠道退款 API → status=1 退款中 → webhook 置 3
   MANUAL_OFFLINE(LinkTrust)→ 仅置 status=1 退款中（不调任何渠道 API）
        → 管理员去 LinkTrust 商户后台手工退款
        → 回站内点「确认已退款」 PUT /api/admin/refunds/{id}/complete → status=3
管理端「拒绝」 → status=2（原样）
status=3 联动（原样）：payment.pay_status=3、booking.pay_status=2、未入住取消 booking_status=4
```

- 新端点 `PUT /api/admin/refunds/{id}/complete`：仅 status=1 且渠道 RefundMode=MANUAL_OFFLINE 可调。
- 管理端退款列表：channel=3 显示「LinkTrust」，同意弹窗提示线下操作步骤，status=1 行出现「确认已退款」按钮。

## 5. 房间锁定与待支付超时释放（需求1的缺口修复）

现状：下单即锁房（booking_status∈{0,1,2} 参与占用判定），**无超时释放**。外跳支付场景下用户弃单会永久占房。

新增 `PendingPayTimeoutJob`（仿 OrphanFileCleanupJob 模板）：
- 配置：`minsu.booking.pay-timeout-minutes`（默认 30，0=禁用，env `BOOKING_PAY_TIMEOUT_MINUTES`）
- `@Scheduled` 每 5 分钟扫描：`pay_status=0 AND booking_status=0 AND create_time < now-超时`
- 取消前防误伤：该订单存在渠道(2/3)待支付 payment 行时，先 `PaymentSyncService.sync` 最后确认一次；同步后已支付 → 不取消
- 取消：booking_status=4（释放占用），与手工取消口径一致（促销名额同样不回补）

## 6. 数据库变更（backend/sql/migrations/migrate-v5-linktrustpay.sql，并入 schema.sql）

无新表新列（merOrderId 复用 pay_no、渠道交易号复用 transaction_no），仅注释口径更新：
- `payment.channel` → '渠道:1模拟 2Stripe 3LinkTrust'
- `payment.pay_method` / `booking` 相关注释 → 增加 '7银联'
- `refund_record.channel` → 同步

## 7. API 契约增量

用户端：
- `GET /api/payments/config` 增量返回：`linktrustEnabled`、`linktrustMethods`（pay_method 编码数组，如 [1,2,7]）
- `POST /api/payments/linktrust/create` {orderNo, payMethod} → {payUrl, merId, merOrderId, amount}
- `POST /api/payments/linktrust/callback`（免登录，表单编码，IPN 专用）

管理端：
- `PUT /api/admin/refunds/{id}/complete`（确认线下退款完成）

## 8. 前端增量

用户端（i18n 9 语种齐全）：
- Payment.vue：`linktrustEnabled` 时渲染 微信/支付宝/银联 选项（含「跳转到 LinkTrust Pay 安全页面」提示）；确认后隐藏表单 POST 新标签页 + 本页跳结果页
- PayResult.vue：轮询超限仍待支付 → 「支付确认中」持续态 + 「我已完成支付，刷新」按钮（替代误报失败）
- dict.js `PAY_METHOD` 增 7=银联

管理端：
- dict.js `PAY_CHANNEL` 增 3=LinkTrust
- RefundList.vue：channel=3 同意弹窗改提示线下流程；status=1 且 channel=3 显示「确认已退款」
- api/refund.js 增 `completeRefund`

## 9. 可扩展性说明（需求3）

未来接入信用卡供应商（或任意新渠道）步骤固定为：
1. 新建 `XxxConfig`（环境变量、isEnabled）+ `XxxPayChannel implements PayChannel`（声明 channel 编码与 RefundMode）
2. `PaymentConfigVO` 暴露该渠道启用状态与方式 → 前端支付页增加选项分支
3. 有回调则加免登录端点，统一落库走 `PaymentStateService` / `PaymentSyncService`
4. 退款按 RefundMode 自动进入正确分支，`RefundService` 无需修改

## 10. 上线与激活

1. 默认未配置 `LINKTRUST_MER_ID` → 渠道不可见，现有 Stripe/Mock 流程零影响
2. 在 LinkTrust 商户后台（http://pay.linktrust-pay.com）「Web(HTMLリンク)決済設定」配置通知 URL：
   `https://seabnb.axionintell.com/api/payments/linktrust/callback`
3. `.env` 填 `LINKTRUST_MER_ID=`（正式店铺ID；测试可用 M810000022）→ `docker compose up -d --build backend web`
4. 确认 `PAYMENT_CURRENCY=jpy`（LinkTrust 仅收日元整数；旧 .env 里的 `STRIPE_CURRENCY` 仍兜底生效）
5. 用测试账户走通：下单 → 跳转支付 → IPN/轮询确认 → 后台同意退款 → LinkTrust 后台手工退款 → 站内确认已退款
