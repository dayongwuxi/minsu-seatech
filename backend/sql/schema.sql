-- =====================================================================
-- 民宿在线预约管理系统 数据库初始化脚本 (MySQL 8.x)
-- 依据《民宿在线预约管理系统基本设计》(19页) 及 32 张画面设计图逐张审阅结果建模
-- 统一约定:
--   * 字符集 utf8mb4, 引擎 InnoDB
--   * deleted   逻辑删除: 0 正常 1 已删除 (MyBatis-Plus @TableLogic)
--   * 状态枚举统一为 TINYINT, 含义见各表注释 (解决设计稿间枚举不一致)
--   * 订单号规则统一: DD + yyyyMMdd + 4位流水 (设计稿 DO/DD/D 前缀不一致, 以 DD 为准)
-- =====================================================================

-- 强制连接字符集为 utf8mb4：docker-entrypoint-initdb.d 执行本脚本时，mysql 客户端会因
-- 容器内 LANG 为空而按 locale 自动选 latin1，导致 UTF-8 中文种子数据被双重编码写入（已踩坑）。
SET NAMES utf8mb4;

CREATE DATABASE IF NOT EXISTS minsu DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE minsu;

-- ---------------------------------------------------------------------
-- 1. 会员/用户域
-- ---------------------------------------------------------------------

-- 会员类型 (admin画面04)
CREATE TABLE member_type (
  id           BIGINT PRIMARY KEY AUTO_INCREMENT,
  type_no      VARCHAR(20)  NOT NULL UNIQUE COMMENT '类型编号 MT001',
  type_name    VARCHAR(50)  NOT NULL COMMENT '类型名称',
  discount     DECIMAL(3,2) NOT NULL DEFAULT 1.00 COMMENT '折扣率 0.95=9.5折',
  benefits     VARCHAR(500) COMMENT '权益说明(多条用;分隔)',
  status       TINYINT NOT NULL DEFAULT 1 COMMENT '0停用 1启用',
  deleted      TINYINT NOT NULL DEFAULT 0,
  create_time  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT '会员类型';

-- 前台用户/会员 (用户端02/03/12, admin画面05)
CREATE TABLE member (
  id             BIGINT PRIMARY KEY AUTO_INCREMENT,
  member_no      VARCHAR(30) NOT NULL UNIQUE COMMENT '会员编号 M+yyyyMMdd+4位流水',
  username       VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
  phone          VARCHAR(20) NOT NULL UNIQUE COMMENT '手机号(纯数字,不限位数)',
  phone_country  VARCHAR(8) DEFAULT NULL COMMENT '国家区号(如+81/+86)',
  password       VARCHAR(100) NOT NULL COMMENT 'BCrypt加密',
  name           VARCHAR(50) COMMENT '会员姓名',
  email          VARCHAR(100),
  avatar         VARCHAR(255),
  member_type_id BIGINT COMMENT 'FK member_type.id',
  stripe_customer_id VARCHAR(64) COMMENT 'Stripe Customer id',
  status         TINYINT NOT NULL DEFAULT 1 COMMENT '0禁用 1正常',
  deleted        TINYINT NOT NULL DEFAULT 0,
  register_time  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_member_type (member_type_id)
) COMMENT '前台会员';

-- ---------------------------------------------------------------------
-- 2. 后台权限域 (admin画面01/03/18)
-- ---------------------------------------------------------------------

CREATE TABLE role (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  role_name   VARCHAR(50) NOT NULL COMMENT '角色名称',
  role_code   VARCHAR(50) NOT NULL UNIQUE COMMENT 'super_admin/oper_admin/service_staff/finance_staff',
  remark      VARCHAR(200) COMMENT '权限范围说明',
  deleted     TINYINT NOT NULL DEFAULT 0,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) COMMENT '后台角色';

CREATE TABLE admin (
  id              BIGINT PRIMARY KEY AUTO_INCREMENT,
  username        VARCHAR(50) NOT NULL UNIQUE COMMENT '管理员账号',
  password        VARCHAR(100) NOT NULL COMMENT 'BCrypt加密',
  name            VARCHAR(50) COMMENT '姓名',
  phone           VARCHAR(20),
  email           VARCHAR(100),
  avatar          VARCHAR(255),
  role_id         BIGINT COMMENT 'FK role.id',
  status          TINYINT NOT NULL DEFAULT 1 COMMENT '0禁用 1启用',
  last_login_time DATETIME,
  deleted         TINYINT NOT NULL DEFAULT 0,
  create_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_admin_role (role_id)
) COMMENT '后台管理员(客服角色亦在此表)';

CREATE TABLE menu (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  parent_id   BIGINT NOT NULL DEFAULT 0 COMMENT '父菜单id, 0为顶级',
  menu_name   VARCHAR(50) NOT NULL,
  icon        VARCHAR(50),
  path        VARCHAR(100) COMMENT '前端路由路径',
  sort        INT NOT NULL DEFAULT 0,
  status      TINYINT NOT NULL DEFAULT 1 COMMENT '0停用 1启用',
  deleted     TINYINT NOT NULL DEFAULT 0,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) COMMENT '后台菜单';

CREATE TABLE role_menu (
  id      BIGINT PRIMARY KEY AUTO_INCREMENT,
  role_id BIGINT NOT NULL,
  menu_id BIGINT NOT NULL,
  UNIQUE KEY uk_role_menu (role_id, menu_id)
) COMMENT '角色-菜单关联';

-- ---------------------------------------------------------------------
-- 3. 房间域 (用户端01/04/05, admin画面06/07/08)
-- ---------------------------------------------------------------------

CREATE TABLE room_type (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  type_no     VARCHAR(30) NOT NULL UNIQUE COMMENT '类型编号 RT+yyyyMMdd+3位流水',
  type_name   VARCHAR(50) NOT NULL COMMENT '大床房/双床房/家庭房/套房/榻榻米房/亲子房',
  description VARCHAR(500) COMMENT '类型说明',
  status      TINYINT NOT NULL DEFAULT 1 COMMENT '0停用 1启用',
  deleted     TINYINT NOT NULL DEFAULT 0,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) COMMENT '房间类型';

CREATE TABLE room (
  id           BIGINT PRIMARY KEY AUTO_INCREMENT,
  room_no      VARCHAR(30) NOT NULL UNIQUE COMMENT '房间编号 RM+yyyyMMdd+3位流水',
  room_name    VARCHAR(100) NOT NULL,
  room_type_id BIGINT NOT NULL COMMENT 'FK room_type.id',
  price        DECIMAL(10,2) NOT NULL COMMENT '现价/晚',
  origin_price DECIMAL(10,2) COMMENT '划线原价/晚',
  max_guests   INT NOT NULL DEFAULT 2 COMMENT '可入住人数',
  area         DECIMAL(6,2) COMMENT '面积m²',
  bed_info     VARCHAR(100) COMMENT '床型描述: 大床1.8m×1张',
  facilities   VARCHAR(500) COMMENT '房间设施, 逗号分隔: 无线WiFi,空调,电视...',
  cover_image  VARCHAR(255) COMMENT '封面图',
  description  VARCHAR(2000) COMMENT '房间说明',
  booking_note VARCHAR(1000) COMMENT '预约须知',
  is_recommend TINYINT NOT NULL DEFAULT 0 COMMENT '首页推荐 0否 1是',
  room_status  TINYINT NOT NULL DEFAULT 0 COMMENT '0可预订 1维修中 2已满房',
  shelf_status TINYINT NOT NULL DEFAULT 1 COMMENT '0已下架 1已上架',
  deleted      TINYINT NOT NULL DEFAULT 0,
  create_time  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_room_type (room_type_id),
  KEY idx_room_status (room_status, shelf_status)
) COMMENT '房间信息';

CREATE TABLE room_image (
  id        BIGINT PRIMARY KEY AUTO_INCREMENT,
  room_id   BIGINT NOT NULL,
  image_url VARCHAR(255) NOT NULL,
  sort      INT NOT NULL DEFAULT 0,
  KEY idx_room_image (room_id)
) COMMENT '房间图片(最多8张)';

CREATE TABLE room_i18n (
  id           BIGINT PRIMARY KEY AUTO_INCREMENT,
  room_id      BIGINT NOT NULL,
  lang         VARCHAR(10) NOT NULL COMMENT '语言码 en/ja/ko/fr/de/es/pt/ru',
  description  VARCHAR(2000) COMMENT '该语言房间说明',
  booking_note VARCHAR(1000) COMMENT '该语言预订须知',
  update_time  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_room_lang (room_id, lang)
) COMMENT '房间说明/须知多语言译文(base 中文在 room 表)';

-- 周边设施 (admin画面09, 用户端首页导航)
CREATE TABLE nearby_facility (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  facility_no VARCHAR(20) NOT NULL UNIQUE COMMENT '设施编号 S001',
  name        VARCHAR(100) NOT NULL,
  type        VARCHAR(30) NOT NULL COMMENT '地铁站/餐饮美食/便利店/停车场/景点...',
  distance    INT COMMENT '距离(米)',
  address     VARCHAR(255),
  image_url   VARCHAR(255),
  status      TINYINT NOT NULL DEFAULT 1 COMMENT '0维护中 1启用',
  deleted     TINYINT NOT NULL DEFAULT 0,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) COMMENT '周边设施';

-- 收藏 (用户端05 收藏房间)
CREATE TABLE favorite (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  member_id   BIGINT NOT NULL,
  room_id     BIGINT NOT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_fav (member_id, room_id)
) COMMENT '房间收藏';

-- ---------------------------------------------------------------------
-- 4. 交易域 (用户端06/07/08/13, admin画面10/11/12/13)
-- 状态机: pay_status 0待支付 1已支付 2已退款
--         booking_status 0待确认 1已确认 2已入住 3已完成(已退房) 4已取消
-- 用户端"已支付"=pay_status=1; "已完成"=booking_status=3
-- ---------------------------------------------------------------------

CREATE TABLE booking (
  id              BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_no        VARCHAR(30) NOT NULL UNIQUE COMMENT '订单编号 DD+yyyyMMdd+4位流水',
  member_id       BIGINT NOT NULL,
  room_id         BIGINT NOT NULL,
  checkin_date    DATE NOT NULL COMMENT '入住日期',
  checkout_date   DATE NOT NULL COMMENT '退房日期',
  nights          INT NOT NULL COMMENT '入住天数(后端计算)',
  guest_count     INT NOT NULL DEFAULT 1 COMMENT '入住人数',
  guest_name      VARCHAR(50) NOT NULL COMMENT '入住人姓名',
  contact_phone   VARCHAR(20) NOT NULL COMMENT '联系电话',
  unit_price      DECIMAL(10,2) NOT NULL COMMENT '下单时房间单价',
  room_fee        DECIMAL(10,2) COMMENT '房费小计=单价×晚数',
  promo_discount  DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '促销优惠额',
  member_discount DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '会员折扣额',
  promotion_id    BIGINT COMMENT '命中的促销活动id',
  promo_code      VARCHAR(30) COMMENT '使用的优惠码',
  discount_amount DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '总优惠额=promo+member',
  total_amount    DECIMAL(10,2) NOT NULL COMMENT '订单总金额(后端重算防篡改)',
  currency        VARCHAR(8) NOT NULL DEFAULT 'cny',
  pay_status      TINYINT NOT NULL DEFAULT 0 COMMENT '0待支付 1已支付 2已退款',
  booking_status  TINYINT NOT NULL DEFAULT 0 COMMENT '0待确认 1已确认 2已入住 3已完成 4已取消',
  user_remark     VARCHAR(500) COMMENT '用户备注',
  admin_remark    VARCHAR(500) COMMENT '管理员备注',
  deleted         TINYINT NOT NULL DEFAULT 0,
  create_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_booking_member (member_id),
  KEY idx_booking_room_date (room_id, checkin_date, checkout_date),
  KEY idx_booking_status (booking_status, pay_status)
) COMMENT '预定记录';

CREATE TABLE payment (
  id             BIGINT PRIMARY KEY AUTO_INCREMENT,
  pay_no         VARCHAR(30) NOT NULL UNIQUE COMMENT '支付流水号 SR+yyyyMMdd+4位流水',
  booking_id     BIGINT NOT NULL,
  member_id      BIGINT NOT NULL,
  amount         DECIMAL(10,2) NOT NULL,
  pay_method     TINYINT NOT NULL COMMENT '1微信 2支付宝 3银行卡 4其他/余额 5PayPay 6PayPal 7银联',
  channel        TINYINT NOT NULL DEFAULT 1 COMMENT '1模拟 2Stripe 3LinkTrust',
  currency       VARCHAR(8) NOT NULL DEFAULT 'cny',
  pay_status     TINYINT NOT NULL DEFAULT 0 COMMENT '0待支付 1成功 2失败 3已退款',
  pay_time       DATETIME,
  transaction_no VARCHAR(64) COMMENT '第三方交易单号',
  stripe_payment_intent_id VARCHAR(64) COMMENT 'Stripe PaymentIntent id',
  create_time    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  pending_flag   TINYINT GENERATED ALWAYS AS (IF(pay_status = 0, 1, NULL)) VIRTUAL COMMENT '并发闸门: 待支付=1 其余NULL',
  UNIQUE KEY uk_payment_pending (booking_id, channel, pending_flag),
  KEY idx_pay_booking (booking_id),
  KEY idx_pay_time (pay_time)
) COMMENT '支付记录(收入管理数据源)';

CREATE TABLE refund_record (
  id            BIGINT PRIMARY KEY AUTO_INCREMENT,
  booking_id    BIGINT NOT NULL,
  member_id     BIGINT NOT NULL,
  refund_amount DECIMAL(10,2) NOT NULL,
  reason        VARCHAR(500) COMMENT '退款原因',
  status        TINYINT NOT NULL DEFAULT 0 COMMENT '0申请中 1已同意(退款中) 2已拒绝 3已退款',
  channel       TINYINT NOT NULL DEFAULT 1 COMMENT '1模拟 2Stripe 3LinkTrust',
  stripe_refund_id VARCHAR(64) COMMENT 'Stripe Refund id',
  apply_time    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  handle_time   DATETIME,
  handler_id    BIGINT COMMENT '处理管理员id',
  KEY idx_refund_booking (booking_id)
) COMMENT '退款记录';

CREATE TABLE checkin_record (
  id            BIGINT PRIMARY KEY AUTO_INCREMENT,
  checkin_no    VARCHAR(30) NOT NULL UNIQUE COMMENT '入住编号 CR+yyyyMMdd+4位流水',
  booking_id    BIGINT NOT NULL,
  member_id     BIGINT NOT NULL,
  room_id       BIGINT NOT NULL,
  guest_name    VARCHAR(50) NOT NULL,
  checkin_time  DATETIME COMMENT '实际入住时间',
  checkout_time DATETIME COMMENT '实际退房时间',
  status        TINYINT NOT NULL DEFAULT 0 COMMENT '0待入住 1已入住 2已退房',
  is_reviewed   TINYINT NOT NULL DEFAULT 0 COMMENT '是否已评价',
  deleted       TINYINT NOT NULL DEFAULT 0,
  create_time   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_checkin_member (member_id),
  KEY idx_checkin_booking (booking_id)
) COMMENT '入住记录';

-- 会员已保存支付方式 (V3, 敏感数据只存 Stripe, 本表仅缓存展示元数据)
CREATE TABLE member_payment_method (
  id           BIGINT PRIMARY KEY AUTO_INCREMENT,
  member_id    BIGINT NOT NULL,
  channel      TINYINT NOT NULL DEFAULT 1 COMMENT '1模拟 2Stripe',
  stripe_payment_method_id VARCHAR(64) COMMENT 'Stripe PaymentMethod id (channel=2)',
  method_type  TINYINT NOT NULL DEFAULT 1 COMMENT '1银行卡 2其他',
  brand        VARCHAR(20) COMMENT '卡品牌 visa/mastercard/jcb/unionpay',
  last4        VARCHAR(4)  COMMENT '卡号后四位',
  exp_month    TINYINT,
  exp_year     SMALLINT,
  holder_name  VARCHAR(50) COMMENT '持卡人(可选)',
  is_default   TINYINT NOT NULL DEFAULT 0 COMMENT '默认支付方式',
  deleted      TINYINT NOT NULL DEFAULT 0,
  create_time  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_mpm_member (member_id, deleted)
) COMMENT '会员已保存支付方式';

-- 展示汇率 (V3, 基准 CNY, 结算一律 CNY)
CREATE TABLE exchange_rate (
  id            BIGINT PRIMARY KEY AUTO_INCREMENT,
  currency_code VARCHAR(8) NOT NULL UNIQUE COMMENT 'USD/EUR/GBP/JPY/CNY',
  currency_name VARCHAR(50) NOT NULL,
  symbol        VARCHAR(8) NOT NULL,
  rate          DECIMAL(16,8) NOT NULL COMMENT '1 CNY 兑换量',
  auto_update   TINYINT NOT NULL DEFAULT 1 COMMENT '1随API自动刷新 0管理员手工锁定',
  source        TINYINT NOT NULL DEFAULT 1 COMMENT '1 API 2 手工',
  status        TINYINT NOT NULL DEFAULT 1 COMMENT '0停用 1启用',
  update_time   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT '展示汇率';

-- 促销活动 (V2, 设计见 docs/支付促销退款设计.md)
CREATE TABLE promotion (
  id              BIGINT PRIMARY KEY AUTO_INCREMENT,
  promo_no        VARCHAR(30) NOT NULL UNIQUE COMMENT '促销编号 PM+yyyyMMdd+4位流水',
  name            VARCHAR(100) NOT NULL COMMENT '活动名称(价格明细中展示)',
  type            TINYINT NOT NULL COMMENT '1百分比折扣 2满减 3长住折扣 4早鸟折扣',
  discount_rate   DECIMAL(4,3) COMMENT '折扣率 0.80=8折 (type=1/3/4)',
  discount_amount DECIMAL(10,2) COMMENT '立减金额 (type=2)',
  threshold_amount DECIMAL(10,2) COMMENT '满减门槛 (type=2)',
  min_nights      INT COMMENT '最少连住晚数 (type=3)',
  advance_days    INT COMMENT '提前预订天数 (type=4)',
  room_id         BIGINT COMMENT '限定房间, NULL=全场',
  coupon_code     VARCHAR(30) COMMENT '优惠码, NULL=自动应用',
  start_date      DATE NOT NULL,
  end_date        DATE NOT NULL,
  usage_limit     INT COMMENT '总使用次数上限, NULL=不限',
  used_count      INT NOT NULL DEFAULT 0,
  status          TINYINT NOT NULL DEFAULT 1 COMMENT '0停用 1启用',
  deleted         TINYINT NOT NULL DEFAULT 0,
  create_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_promo_active (status, start_date, end_date),
  KEY idx_promo_code (coupon_code)
) COMMENT '促销活动';

-- ---------------------------------------------------------------------
-- 5. 内容与互动域
-- ---------------------------------------------------------------------

-- 评价 (用户端05/16, admin画面14)
CREATE TABLE review (
  id             BIGINT PRIMARY KEY AUTO_INCREMENT,
  review_no      VARCHAR(30) NOT NULL UNIQUE COMMENT '评价编号 R+yyyyMMdd+4位流水',
  member_id      BIGINT NOT NULL,
  room_id        BIGINT NOT NULL,
  booking_id     BIGINT NOT NULL COMMENT '一单一评',
  rating         DECIMAL(2,1) NOT NULL COMMENT '0.5~5.0 支持半星',
  content        VARCHAR(1000),
  audit_status   TINYINT NOT NULL DEFAULT 0 COMMENT '0待审核 1已通过 2未通过',
  reply_content  VARCHAR(500) COMMENT '商家回复',
  reply_time     DATETIME,
  reply_admin_id BIGINT,
  deleted        TINYINT NOT NULL DEFAULT 0,
  create_time    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_review_booking (booking_id),
  KEY idx_review_room (room_id, audit_status)
) COMMENT '评价记录';

-- 投诉反馈 (用户端12, admin画面15)
CREATE TABLE feedback (
  id            BIGINT PRIMARY KEY AUTO_INCREMENT,
  feedback_no   VARCHAR(30) NOT NULL UNIQUE COMMENT '反馈编号 FB+yyyyMMdd+4位流水',
  member_id     BIGINT NOT NULL,
  type          TINYINT NOT NULL COMMENT '1住宿服务 2设施问题 3订单问题 4环境问题 5其他',
  title         VARCHAR(100) NOT NULL,
  content       VARCHAR(2000) NOT NULL,
  images        VARCHAR(1000) COMMENT '图片URL,逗号分隔',
  status        TINYINT NOT NULL DEFAULT 0 COMMENT '0待处理 1处理中 2已处理',
  reply_content VARCHAR(1000),
  reply_time    DATETIME,
  handler_id    BIGINT COMMENT '处理管理员id',
  deleted       TINYINT NOT NULL DEFAULT 0,
  create_time   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_feedback_member (member_id),
  KEY idx_feedback_status (status)
) COMMENT '投诉反馈';

-- 公告 (用户端09/10, admin画面16)
CREATE TABLE notice (
  id             BIGINT PRIMARY KEY AUTO_INCREMENT,
  notice_no      VARCHAR(30) NOT NULL UNIQUE COMMENT '公告编号 GG+yyyyMMdd+4位流水',
  title          VARCHAR(200) NOT NULL,
  summary        VARCHAR(500) COMMENT '摘要',
  cover_image    VARCHAR(255),
  content        TEXT COMMENT '正文(富文本)',
  publish_status TINYINT NOT NULL DEFAULT 0 COMMENT '0草稿/已下架 1已发布',
  publish_time   DATETIME,
  creator_id     BIGINT COMMENT '创建管理员id',
  creator_name   VARCHAR(50),
  deleted        TINYINT NOT NULL DEFAULT 0,
  create_time    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_notice_publish (publish_status, publish_time)
) COMMENT '公告';

-- 在线客服 (用户端11, admin画面17)
CREATE TABLE cs_session (
  id             BIGINT PRIMARY KEY AUTO_INCREMENT,
  member_id      BIGINT NOT NULL,
  admin_id       BIGINT COMMENT '接入客服(admin表)',
  source_channel VARCHAR(30) DEFAULT 'web' COMMENT '来源渠道: web/微信小程序',
  status         TINYINT NOT NULL DEFAULT 0 COMMENT '0排队中 1会话中 2已结束',
  last_message   VARCHAR(200),
  last_time      DATETIME,
  unread_admin   INT NOT NULL DEFAULT 0 COMMENT '客服未读数',
  unread_member  INT NOT NULL DEFAULT 0 COMMENT '用户未读数',
  start_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  end_time       DATETIME,
  KEY idx_cs_member (member_id),
  KEY idx_cs_status (status, last_time)
) COMMENT '客服会话';

CREATE TABLE cs_message (
  id           BIGINT PRIMARY KEY AUTO_INCREMENT,
  session_id   BIGINT NOT NULL,
  sender_type  TINYINT NOT NULL COMMENT '1会员 2客服',
  sender_id    BIGINT NOT NULL,
  content_type TINYINT NOT NULL DEFAULT 1 COMMENT '1文本 2图片 3文件',
  content      VARCHAR(2000) NOT NULL,
  is_read      TINYINT NOT NULL DEFAULT 0,
  create_time  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_msg_session (session_id, create_time)
) COMMENT '客服消息';

CREATE TABLE cs_quick_reply (
  id       BIGINT PRIMARY KEY AUTO_INCREMENT,
  question VARCHAR(200) NOT NULL COMMENT '常见问题',
  answer   VARCHAR(1000) NOT NULL COMMENT '快捷回复内容',
  sort     INT NOT NULL DEFAULT 0,
  deleted  TINYINT NOT NULL DEFAULT 0
) COMMENT '客服快捷回复';

-- ---------------------------------------------------------------------
-- 6. 系统域 (admin画面18)
-- ---------------------------------------------------------------------

CREATE TABLE banner (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  title       VARCHAR(100) NOT NULL,
  image_url   VARCHAR(255) NOT NULL,
  link_url    VARCHAR(255) COMMENT '跳转链接',
  sort        INT NOT NULL DEFAULT 0,
  status      TINYINT NOT NULL DEFAULT 1 COMMENT '0停用 1启用',
  deleted     TINYINT NOT NULL DEFAULT 0,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) COMMENT '首页轮播图';

CREATE TABLE sys_config (
  id           BIGINT PRIMARY KEY AUTO_INCREMENT,
  config_name  VARCHAR(100) NOT NULL,
  config_key   VARCHAR(50)  NOT NULL UNIQUE,
  config_value VARCHAR(500),
  update_time  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT '系统参数';

CREATE TABLE operation_log (
  id             BIGINT PRIMARY KEY AUTO_INCREMENT,
  log_type       TINYINT NOT NULL COMMENT '1登录 2新增 3修改 4删除 5其他',
  admin_id       BIGINT,
  operator_name  VARCHAR(50),
  content        VARCHAR(500) COMMENT '操作内容',
  ip             VARCHAR(50),
  create_time    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_log_time (create_time)
) COMMENT '操作日志';

-- =====================================================================
-- 种子数据
-- 管理员初始账号由后端启动时 DataInitializer 自动创建 (admin/admin123, BCrypt),
-- 避免在 SQL 中硬编码哈希。
-- =====================================================================

INSERT INTO role (role_name, role_code, remark) VALUES
 ('超级管理员','super_admin','全部权限'),
 ('运营管理员','oper_admin','运营管理'),
 ('客服人员','service_staff','客服相关'),
 ('财务人员','finance_staff','财务相关');

INSERT INTO menu (parent_id, menu_name, icon, path, sort) VALUES
 (0,'后台首页','Odometer','/dashboard',1),
 (0,'会员类型管理','Medal','/member-types',2),
 (0,'会员管理','User','/members',3),
 (0,'房间类型管理','Collection','/room-types',4),
 (0,'房间信息管理','House','/rooms',5),
 (0,'周边设施管理','MapLocation','/facilities',6),
 (0,'预定记录管理','Tickets','/bookings',7),
 (0,'入住记录管理','Key','/checkin-records',8),
 (0,'收入管理','Money','/incomes',9),
 (0,'评价记录管理','Star','/reviews',10),
 (0,'投诉反馈管理','Warning','/feedbacks',11),
 (0,'公告管理','Bell','/notices',12),
 (0,'在线客服管理','ChatDotRound','/customer-service',13),
 (0,'系统管理','Setting','/system',14);

INSERT INTO member_type (type_no, type_name, discount, benefits) VALUES
 ('MT001','普通会员',0.95,'享受房费9.5折优惠;积分可兑换优惠券;生日专属优惠券'),
 ('MT002','VIP会员',0.85,'免费早餐(部分房型);延迟退房至14:00;积分加倍'),
 ('MT003','长住会员',0.75,'连住7晚及以上额外9折;免费洗衣服务;延迟退房至16:00');

INSERT INTO room_type (type_no, type_name, description) VALUES
 ('RT202505240001','大床房','配备1张大床(1.8m/2.0m), 适合情侣或单人入住'),
 ('RT202505240002','双床房','配备2张单人床(1.2m), 适合朋友或同事出行'),
 ('RT202505240003','家庭房','空间宽敞, 配备大床+单人床, 适合家庭入住'),
 ('RT202505240004','套房','独立客厅与卧室, 尊享舒适体验'),
 ('RT202505240005','榻榻米房','日式榻榻米风格, 适合喜欢日式体验的客人'),
 ('RT202505240006','亲子房','配备儿童设施, 适合亲子出行');

INSERT INTO room (room_no, room_name, room_type_id, price, origin_price, max_guests, area, bed_info, facilities, description, is_recommend, cover_image) VALUES
 ('RM20250524001','海景大床房',1,16400.00,18800.00,2,28,'大床1.8m×1张','无线WiFi,空调,电视,独立卫浴,热水器,吹风机,洗漱用品,电水壶,阳台,衣柜','超大落地窗, 直面无敌海景, 观日出日落',1,'/files/searoom.png'),
 ('RM20250524002','山景双床房',2,12500.00,14400.00,2,26,'单人床1.2m×2张','无线WiFi,空调,电视,独立卫浴,热水器,吹风机','推窗见山, 清幽宁静',1,'/files/mountainroom.png'),
 ('RM20250524003','庭院家庭房',3,21200.00,24000.00,4,45,'大床1.8m×1张+单人床1.2m×1张','无线WiFi,空调,电视,独立卫浴,热水器,吹风机,迷你冰箱','带独立小庭院, 适合全家度假',1,'/files/cozyfamily.png');

INSERT INTO nearby_facility (facility_no, name, type, distance, address) VALUES
 ('S001','海湾地铁站(2号线)','地铁站',350,'海口市美兰区海秀东路88号'),
 ('S002','渔家海鲜餐厅','餐饮美食',520,'海口市美兰区海秀东路120号'),
 ('S003','全家便利店(海秀店)','便利店',280,'海口市美兰区海秀东路66号'),
 ('S004','海秀公共停车场','停车场',450,'海口市美兰区海秀东路100号');

-- 轮播图三张图片来自 backend/seed-assets/，需随部署拷入 uploads 卷:
--   docker cp backend/seed-assets/<名称>.png minsu-backend:/app/uploads/
INSERT INTO banner (title, image_url, link_url, sort) VALUES
 ('温馨家庭房','/files/cozyfamily.png','/rooms',1),
 ('山景房','/files/mountainroom.png','/rooms',2),
 ('海景房','/files/searoom.png','/rooms',3);

INSERT INTO room_image (room_id, image_url, sort) VALUES
 (1,'/files/searoom.png',1),
 (2,'/files/mountainroom.png',1),
 (3,'/files/cozyfamily.png',1);

INSERT INTO sys_config (config_name, config_key, config_value) VALUES
 ('网站名称','site_name','民宿预约管理系统'),
 ('网站LOGO','site_logo','/files/logo.png'),
 ('客服联系电话','site_phone','13800008888'),
 ('每页显示条数','page_size','10'),
 ('默认时区','site_timezone','Asia/Shanghai'),
 ('入住时间','checkin_time','14:00'),
 ('退房时间','checkout_time','12:00');

INSERT INTO cs_quick_reply (question, answer, sort) VALUES
 ('入住时间和退房时间是几点?','您好, 入住时间为14:00后, 退房时间为次日12:00前。',1),
 ('民宿提供早餐吗?','您好, VIP会员部分房型含免费早餐, 其他房型可单独购买。',2),
 ('可以携带宠物吗?','您好, 很抱歉本民宿暂不支持携带宠物入住。',3),
 ('如何到达民宿? 有停车位吗?','您好, 距海湾地铁站350米, 附近有海秀公共停车场。',4),
 ('取消预订的政策是怎样的?','您好, 入住前24小时可免费取消, 之后取消将收取首晚房费。',5);

-- 基准 = 站点结算货币(PAYMENT_CURRENCY=jpy)，基准行锁定 rate=1；其余行启动时自动刷新
INSERT INTO exchange_rate (currency_code, currency_name, symbol, rate, auto_update, source) VALUES
 ('JPY','日元','JP¥',1.00000000,0,2),
 ('CNY','人民币','¥',0.04140000,1,1),
 ('USD','美元','$',0.00610000,1,1),
 ('EUR','欧元','€',0.00540000,1,1),
 ('GBP','英镑','£',0.00460000,1,1);

INSERT INTO promotion (promo_no, name, type, discount_rate, min_nights, start_date, end_date, status) VALUES
 ('PM202607080001','长住 7 晚 9 折', 3, 0.900, 7, '2026-07-01', '2027-06-30', 1);
INSERT INTO promotion (promo_no, name, type, discount_rate, advance_days, start_date, end_date, status) VALUES
 ('PM202607080002','提前 30 天早鸟 95 折', 4, 0.950, 30, '2026-07-01', '2027-06-30', 1);
INSERT INTO promotion (promo_no, name, type, discount_amount, threshold_amount, coupon_code, start_date, end_date, usage_limit, status) VALUES
 ('PM202607080003','夏季优惠码 SUMMER100 满24000减2400', 2, 2400.00, 24000.00, 'SUMMER100', '2026-07-01', '2026-09-30', 500, 1);

INSERT INTO notice (notice_no, title, summary, content, publish_status, publish_time, creator_name) VALUES
 ('GG202506010001','端午节活动通知','端午节活动即将开始','亲爱的住客: 端午节活动即将开始, 活动期间预订可享受8折优惠, 欢迎体验!',1,'2026-06-01 10:00:00','admin'),
 ('GG202505200001','关于暑期价格调整的通知','暑期旺季价格调整说明','尊敬的住客: 因暑期旺季, 部分房型价格将有所调整, 详情见房间页面。',1,'2026-05-20 09:00:00','admin');
