<div align="center">
  <h1>🌆 CityHub - 本地生活服务平台</h1>
  <p>
    <img src="https://img.shields.io/badge/Spring%20Boot-2.3.12-green" alt="Spring Boot">
    <img src="https://img.shields.io/badge/RabbitMQ-3.x-orange" alt="RabbitMQ">
    <img src="https://img.shields.io/badge/Redis-7.0-red" alt="Redis">
    <img src="https://img.shields.io/badge/Java-1.8-blue" alt="Java">
    <img src="https://img.shields.io/badge/Redisson-3.13-brightgreen" alt="Redisson">
    <img src="https://img.shields.io/badge/Caffeine-2.9.3-blue" alt="Caffeine">
    <img src="https://img.shields.io/badge/MyBatis--Plus-3.4-purple" alt="MyBatis-Plus">
    <img src="https://img.shields.io/badge/License-MIT-yellow" alt="License">
  </p>
  <p>
    <strong>类"大众点评"本地生活服务平台 · 高并发秒杀 · 多级缓存 · 异步架构</strong>
  </p>
</div>

---

## 📋 目录

- [项目简介](#项目简介)
- [功能总览](#功能总览)
  - [一、用户模块](#一用户模块)
  - [二、商铺模块](#二商铺模块)
  - [三、商铺分类](#三商铺分类)
  - [四、优惠券模块](#四优惠券模块)
  - [五、秒杀模块](#五秒杀模块)
  - [六、博客笔记模块](#六博客笔记模块)
  - [七、关注模块](#七关注模块)
- [技术架构](#技术架构)
  - [核心技术栈](#核心技术栈)
  - [架构亮点](#架构亮点)
  - [Redis 特性应用](#redis-特性应用)
- [数据库设计](#数据库设计)
- [快速启动](#快速启动)

---

## 项目简介

**CityHub** 是一个参考"大众点评"设计的本地生活服务平台，覆盖了从商铺浏览、优惠券秒杀到社交分享的完整业务链路。项目以 **高并发秒杀** 为核心场景，通过 JWT、Redis、RabbitMQ、Redisson 和 Caffeine 处理认证、缓存与并发问题。

> 实现状态以当前代码为准。文中未接入代码的支付、关单、UV 等内容仅作为设计记录，不视为已实现功能。

> 详细的设计取舍与踩坑复盘请参阅 [README.md](./README.md)。

---

## 🚀 功能总览

### 一、用户模块

| 功能 | 请求方式 | 接口路径 | 说明 |
|------|---------|---------|------|
| 发送验证码 | `POST` | `/user/code` | 输入手机号，发送验证码（有效期2分钟） |
| 用户登录 | `POST` | `/user/login` | 手机号 + 验证码登录，返回带 userId、jti、签发和过期时间的 HS256 JWT |
| 用户登出 | `POST` | `/user/logout` | 退出登录，清除登录态 |
| 获取当前用户 | `GET` | `/user/me` | 获取当前登录用户基本信息 |
| 用户详细资料 | `GET` | `/user/info/{id}` | 获取用户详细信息（头像、签名等） |
| 用户基本信息 | `GET` | `/user/{id}` | 获取指定用户的公开信息 |

### 二、商铺模块

| 功能 | 请求方式 | 接口路径 | 说明 |
|------|---------|---------|------|
| 查询商铺详情 | `GET` | `/shop/{id}` | 两级缓存 + 逻辑过期 + Redisson 重建锁，缓存空值防穿透 |
| 新增商铺 | `POST` | `/shop` | 添加新商铺 |
| 更新商铺 | `PUT` | `/shop` | 修改商铺信息，并更新缓存 |
| 按类型查询 | `GET` | `/shop/of/type` | 按类型分页查询，支持 GEO 距离排序 |
| 按名称搜索 | `GET` | `/shop/of/name` | 按商铺名称关键字模糊搜索 |

### 三、商铺分类

| 功能 | 请求方式 | 接口路径 | 说明 |
|------|---------|---------|------|
| 分类列表 | `GET` | `/shop-type/list` | 获取所有商铺分类（按 sort 排序），结果缓存至 Redis |

### 四、优惠券模块

| 功能 | 请求方式 | 接口路径 | 说明 |
|------|---------|---------|------|
| 新增普通券 | `POST` | `/voucher` | 添加普通优惠券 |
| 新增秒杀券 | `POST` | `/voucher/seckill` | 添加秒杀优惠券（含库存、开始/结束时间） |
| 商铺优惠券列表 | `GET` | `/voucher/list/{shopId}` | 查询指定商铺的所有可用优惠券 |

### 五、秒杀模块

| 功能 | 请求方式 | 接口路径 | 说明 |
|------|---------|---------|------|
| 秒杀下单 | `POST` | `/voucher-order/seckill/{id}` | **核心功能**：高并发秒杀，异步处理订单 |

> **秒杀流程**：Redis + Lua 判断资格（库存 & 一人一单）→ 同步等待 MQ publisher confirm → 消费者按用户获取 Redisson 锁 → 事务内扣库存并写订单；消息失败会原子补偿 Redis，定时任务会重新投递未落库订单。

**失败处理**：正常队列最多重试 3 次，仍失败则拒绝并路由到死信队列；死信消费者最终失败时保留 `seckill:pending:v2:{voucherId}`，由每分钟运行一次的对账任务重新投递。正常队列不再设置 10 秒消息 TTL，避免把正常削峰积压误判为死信。

**接口限流**：秒杀接口使用 Redis ZSet 滑动窗口，默认按 IP 每 10 秒最多 5 次请求；切面支持全局、用户、IP 三种维度。

### 六、博客笔记模块

| 功能 | 请求方式 | 接口路径 | 说明 |
|------|---------|---------|------|
| 发布笔记 | `POST` | `/blog` | 发布探店笔记/博文 |
| 点赞/取消 | `PUT` | `/blog/like/{id}` | 笔记点赞或取消点赞，Redis ZSet 记录点赞用户 |
| 我的笔记 | `GET` | `/blog/of/me` | 查询当前用户发布的所有笔记 |
| 热门笔记 | `GET` | `/blog/hot` | 分页查询热门笔记（按点赞数降序） |
| 笔记详情 | `GET` | `/blog/{id}` | 查询单条笔记完整内容 |
| 点赞用户列表 | `GET` | `/blog/likes/{id}` | 查看某笔记的点赞用户列表（按时间排序） |
| 用户笔记列表 | `GET` | `/blog/of/user` | 查询指定用户发布的所有笔记 |
| **关注推送** | `GET` | `/blog/of/follow` | 滚动查询关注用户的最新笔记（Redis 收件箱 + 数据库 outbox 补偿） |

### 七、关注模块

| 功能 | 请求方式 | 接口路径 | 说明 |
|------|---------|---------|------|
| 关注/取关 | `PUT` | `/follow/{id}/{isFollow}` | 关注或取消关注用户 |
| 是否已关注 | `GET` | `/follow/or/not/{id}` | 判断当前用户是否已关注目标用户 |
| **共同关注** | `GET` | `/follow/common/{id}` | 查询与指定用户的共同关注（Redis Set 交集运算） |

---

## 🛠 技术架构

### 核心技术栈

| 层级 | 技术 | 用途 |
|------|------|------|
| **基础框架** | Spring Boot 2.3.12 | 应用主框架 |
| **ORM** | MyBatis-Plus 3.4 | 数据持久化，简化 CRUD |
| **关系型数据库** | MySQL 8.0 | 核心业务数据存储 |
| **分布式缓存** | Redis 7.0 | 热点缓存、秒杀预占、临时登录态 |
| **本地缓存** | Caffeine 2.9.3 | 进程内 L1 缓存，减少 Redis 压力 |
| **分布式锁** | Redisson 3.13 | 缓存冷 Key 重建和分类缓存互斥 |
| **消息队列** | RabbitMQ | 秒杀异步下单，削峰填谷 |
| **工具库** | Hutool 5.7 | 通用工具类 |
| **连接池** | Lettuce + Commons-Pool2 | Redis 连接管理 |

### 架构亮点

#### 🔥 热点缓存 — 两级缓存架构

```
请求 → Caffeine(本地 L1) → Redis(分布式 L2) → MySQL(数据库)
       TTL 5min              TTL 30min
```

- **L1 本地缓存**：Caffeine 缓存热点数据，纳秒级响应，容量上限 1000 条，写入后 5 分钟过期
- **L2 分布式缓存**：商铺缓存使用逻辑过期时间，不设置热点 Key 的物理 TTL；空值缓存 2 分钟
- **缓存击穿**：逻辑过期后返回旧值，后台 Redisson 锁保证只有一个线程异步重建
- **缓存穿透**：采用**缓存空值方案**，Redis 和 Caffeine 均回填空值标记，搭配短 TTL 避免内存浪费
- **缓存更新**：数据变更时通过 `invalidateTwoLevel` 同步清除 L1 和 L2 缓存，缩短不一致窗口

#### ⚡ 高并发秒杀系统

秒杀演进经历了三个阶段：

| 阶段 | 方案 | 问题 |
|------|------|------|
| 1 | DB 悲观锁（for update） | 请求串行化，连接池打满 |
| 2 | DB 乐观锁（CAS） | 数据库扛不住，JVM 锁集群失效 |
| 3 | **Redis + Lua + MQ（最终方案）** | ✅ 毫秒级判断 + 异步写库 |

**最终方案流程：**
1. Lua 脚本原子执行：**判断库存** → **校验一人一单** → **扣减库存**
2. 校验通过 → 发送 RabbitMQ 消息 → 立即返回"排队中"
3. 消费者异步写入 MySQL 订单表

**幂等与补偿**：订单 ID 为主键，`(user_id, voucher_id)` 唯一索引保证一人一单；Redis 保存用户到订单号的预占映射，发布失败时原子回滚库存和资格，定时任务根据 pending Hash 重新投递。

#### 🔒 分布式锁

- **Redisson 分布式锁**：用于缓存冷 Key 重建和分类缓存初始化
- **滑动窗口限流**：`@RateLimit` + AOP + Redis ZSet + Lua，支持全局、用户、IP 维度

### Redis 特性应用

| 业务场景 | Redis 数据结构 | 核心命令 |
|---------|---------------|---------|
| 登录态 | String | SET / GET / EXPIRE |
| 点赞排行榜 | ZSet | ZADD / ZSCORE / ZRANGE |
| 共同关注 | Set | SINTER（交集） |
| 附近商铺 | GEO | GEORADIUS / GEOSEARCH |
| 用户签到 | BitMap | SETBIT / GETBIT |
| 秒杀库存 | String + Lua | EVAL / DECR |
| 关注收件箱 | ZSet | ZREVRANGEBYSCORE |
| 全局 ID 生成器 | String | INCR（按天自增） |

---

## 📊 数据库设计

| 表名 | 说明 | 主要字段 |
|------|------|---------|
| `tb_user` | 用户表 | id, phone, password, nick_name, icon |
| `tb_user_info` | 用户详情表 | user_id, city, introduce, fans, followee |
| `tb_shop` | 商铺表 | id, name, type_id, images, area, address, x, y |
| `tb_shop_type` | 商铺类型表 | id, name, icon, sort |
| `tb_voucher` | 优惠券表 | id, shop_id, title, type, value, pay_value |
| `tb_seckill_voucher` | 秒杀库存表 | voucher_id, stock, begin_time, end_time |
| `tb_voucher_order` | 优惠券订单表 | id, user_id, voucher_id, status, create_time |
| `tb_blog` | 笔记表 | id, shop_id, user_id, title, content, liked |
| `tb_blog_comments` | 笔记评论表 | id, user_id, blog_id, content, parent_id |
| `tb_blog_like` | 笔记点赞关系表 | id, blog_id, user_id, create_time |
| `tb_follow` | 关注关系表 | id, user_id, follow_user_id, create_time |
| `tb_feed_outbox` | Feed 分发补偿表 | blog_id, create_time |

---

## 🚀 快速启动

### 环境要求

- JDK 8 或 JDK 17（当前工具链已用 JDK 17 验证；旧版 Lombok 与 JDK 21 不兼容）
- MySQL 8.0+
- Redis 7.0+
- RabbitMQ 3.x

### 启动步骤

```bash
# 1. 全量初始化数据库
mysql -u root -p < src/main/resources/db/hmdp.sql

# 或者对已有数据库执行增量迁移
mysql -u root -p dingping < src/main/resources/db/hmdp_hardening.sql

# 2. 通过环境变量配置连接信息
export MYSQL_USERNAME=root
export MYSQL_PASSWORD=your_password
export RABBITMQ_USERNAME=guest
export RABBITMQ_PASSWORD=guest
export JWT_SECRET=replace_with_a_long_random_secret

# 3. 启动项目
mvn spring-boot:run
# 或直接运行 HmDianPingApplication.java

# 4. 访问服务
http://localhost:8081
```

---

## 📄 License

[MIT License](./LICENSE)
