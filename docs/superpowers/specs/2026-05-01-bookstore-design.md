# 智能书店系统 设计文档

> 版本：v1.1  
> 日期：2026-05-01  
> 类型：课程作业 / 毕业设计

---

## 1. 项目概述

### 1.1 项目定位

本项目是一个面向高校毕设/课程作业场景的 **图书电商网站**，核心差异点是引入两个 AI 智能模块：

- **智能客服**：用户用自然语言描述需求（如"想看一本讲商业谈判的书，300 页以内"），系统返回精准的书籍推荐与理由
- **智能推荐**：基于用户的搜索/收藏/购买历史，主动推荐其感兴趣的书籍

技术架构按企业开发规范设计（多模块 Maven、对象分层、Flyway 自动迁移、OSS 文件托管、RAG 检索增强生成），目标是答辩时既能展示完整商城业务能力，又能突出 AI 模块的工程深度。

### 1.2 目标用户

- **普通用户**：注册登录、浏览/搜索/购买书籍、AI 对话找书、查看推荐
- **管理员**：管理书籍/分类/订单/用户、查看销售统计、配置首页 Banner

### 1.3 核心功能模块

| 类别 | 模块 |
|---|---|
| 账号体系 | 注册、登录、JWT 双 Token、个人信息、修改密码、修改头像 |
| 商品 | 书籍列表、详情、分类、搜索、热销榜、新书榜 |
| 交易 | 购物车、订单、模拟支付、确认收货 |
| 互动 | 收藏夹、书籍评价（含图片）、多收货地址 |
| **营销** | **优惠券（领取/使用/计算最优券）、秒杀活动** |
| **AI** | **智能客服 RAG 对话**、**个性化推荐** |
| 管理 | 书籍/分类/订单/用户/优惠券/秒杀活动管理、数据看板、操作日志 |

### 1.4 核心特色（论文亮点）

1. 多模块 Maven 工程，AI 独立可微服务化
2. 对象分层 PO/DTO/VO/BO/Query + MapStruct 编译期转换
3. Flyway 数据库版本管理
4. JWT 双 Token + Redis 黑名单
5. RAG 检索增强生成 + 多轮对话上下文改写
6. 加权多源用户画像 + 余弦相似度 Top-K 推荐
7. SSE 流式返回 + 打字机效果
8. OSS 前端直传 + STS 临时凭证
9. Filter + Interceptor + AOP 三层拦截
10. ECharts 数据可视化
11. **Redisson 一站式分布式协同**：分布式锁 / 信号量 / 限流器 / 延时队列 / Stream MQ 统一在同一 SDK
12. **Redis Stream 消息队列**：Consumer Group + ACK + PEL + 死信 Stream，零新增组件
13. **秒杀热路径无 synchronized**：`DECR` 预减库存 + Stream 异步落库 + `RDelayedQueue` 5 分钟自动回滚
14. **优惠券防超发**：`RSemaphore` 信号量原子控总量，无需显式锁

---

## 2. 技术栈

### 2.1 后端

| 组件 | 版本 | 用途 |
|---|---|---|
| Java | 17 | 开发语言 |
| Spring Boot | 3.x | 基础框架 |
| Maven | 3.9+ | 多模块工程管理 |
| MyBatis-Plus | 3.5+ | ORM |
| MySQL | 8.0+ | 主数据库 |
| Redis | 7+ | 缓存 / Token 黑名单 / 限流 / 会话 / Stream MQ |
| 阿里云 OSS | — | 图片文件托管 |
| 通义千问 DashScope | qwen-plus + text-embedding-v3 | AI 对话 + 向量化 |
| **Redisson** | **3.27+** | **分布式锁 / 信号量 / 限流器 / 延时队列 / Stream 客户端，一站式分布式协同** |
| Flyway | 9+ | 数据库版本管理 |
| MapStruct | 1.5+ | 对象转换 |
| JWT (jjwt) | 0.12+ | Token 签发与校验 |
| Caffeine | 3+ | 本地缓存（向量集合） |
| Knife4j | 4+ | 接口文档（基于 OpenAPI 3） |
| Logback | — | 日志 |
| JUnit 5 + Mockito | — | 单元测试 |
| Testcontainers | — | 集成测试 |

### 2.2 前端

| 组件 | 版本 | 用途 |
|---|---|---|
| Vue | 3 | 框架 |
| Vite | 5 | 构建工具 |
| Element Plus | 2.x | UI 组件库 |
| Pinia | 2 | 状态管理 |
| Vue Router | 4 | 路由 |
| Axios | 1.x | HTTP 客户端 |
| ali-oss | 6.x | OSS 直传 SDK |
| ECharts | 5.x | 管理后台图表（仅管理端） |

### 2.3 部署

| 组件 | 版本 | 用途 |
|---|---|---|
| Nginx | 1.24+ | 静态托管 + 反向代理 |
| systemd | — | 后端进程托管 |
| Linux | Ubuntu 22.04 / CentOS 8 | 部署服务器 |
| Let's Encrypt | — | HTTPS 证书 |

### 2.4 视觉风格

- **整体风格**：暖色书店风（C 方案）
- **色板**：
  - 主色 `#d68a4f`（暖橙）
  - 次色 `#f6a36f`
  - 页面底色 `#fff8f1`
  - 次级底色 `#fff3e3`
  - 边框 `#f0e0cc`
  - 主文字 `#5b3a17`（深棕）
  - 次文字 `#a07a4f`（米棕）
  - 渐变 `linear-gradient(135deg, #f7c89c, #d68a4f)`
- **登录页**：左右分栏布局（左品牌图 + 右表单）
- **首页**：经典电商型（顶部导航 + Banner + 多分类推荐 + 右下 AI 浮动球）

---

## 3. 系统架构

### 3.1 高层架构图

```
┌──────────────────────────────────────────────────────────────┐
│  浏览器（用户 / 管理员）                                      │
└──────────────┬──────────────────────────┬────────────────────┘
               │ HTTPS                    │ HTTPS
               ▼                          ▼
   ┌────────────────────┐       ┌────────────────────┐
   │  Nginx 站点 1      │       │  Nginx 站点 2      │
   │  user-frontend     │       │  admin-frontend    │
   │  Vue3+ElementPlus  │       │  Vue3+ElementPlus  │
   └─────────┬──────────┘       └─────────┬──────────┘
             │  /api/* 反向代理            │  /admin-api/* 反向代理
             └─────────────┬───────────────┘
                           ▼
             ┌──────────────────────────────┐
             │  Spring Boot 后端（Maven）   │
             │   ─ JWT 鉴权过滤器           │
             │   ─ 业务模块                 │
             │   ─ AI 模块（RAG + 推荐）    │
             │   ─ 营销模块（优惠券+秒杀）  │
             │   ─ 全局异常 / 统一响应      │
             └─┬────────┬──────────┬────────┘
               │        │          │
        ┌──────▼─┐  ┌───▼─────────────────┐  ┌──▼──────────────┐
        │ MySQL8 │  │ Redis 7             │  │ 通义千问 API    │
        │ 业务+   │  │ ─ 缓存 / 黑名单     │  │ qwen-plus       │
        │ 向量    │  │ ─ Redisson         │  │ + embedding-v3  │
        │        │  │   锁/信号量/限流     │  └─────────────────┘
        │        │  │ ─ Stream MQ         │  ┌─────────────────┐
        │        │  │   秒杀异步下单       │  │ 阿里云 OSS      │
        │        │  │ ─ DelayedQueue      │  │ 图片文件托管    │
        │        │  │   超时回滚库存       │  └─────────────────┘
        └────────┘  └─────────────────────┘
```

### 3.2 关键架构决策

| 决策 | 选择 | 原因 |
|---|---|---|
| 前后端分离方式 | 两个独立 Vue 项目 | 安全边界清晰，可独立部署，代码不互相污染 |
| 后端模块组织 | 单 jar + Maven 多模块（8 子模块） | 兼顾职责分离与部署简单 |
| 数据库 | MySQL 8 | 主流、生态成熟 |
| 向量存储 | MySQL JSON 字段 + Java 端余弦计算 | 数据量级（< 几千本）够用，无需引入额外向量库 |
| 缓存 | Redis 7 | Token 黑名单、推荐结果、限流计数 |
| **消息队列** | **Redis Stream + Redisson 客户端** | **零新增组件；`RDelayedQueue` 原生延时消息直接对接"5 分钟未支付回滚库存"；毕设级别流量充裕** |
| **分布式协同** | **Redisson** | **锁 / 信号量 / 限流器 / 延时队列 / Stream 一站式，避免多组件拼凑** |
| **秒杀防超卖** | **Redis DECR 预减 + DB 乐观锁兜底** | **热路径全程在内存原子操作，无 `synchronized`，性能高且支持集群** |
| 文件存储 | 阿里云 OSS | 减轻服务器带宽压力，CDN 友好 |
| 上传方式 | 前端直传 + STS 临时凭证 | 文件不经后端，节省带宽 |
| AI 服务 | 通义千问 DashScope | 国内可访问、对中文友好、提供 embedding |
| 数据库迁移 | Flyway | 自动化、可追溯 |
| 对象转换 | MapStruct | 编译期生成，无反射开销 |

### 3.3 拦截/横切设计

#### Filter 层（早期，Spring MVC 之前）
| Filter | 职责 |
|---|---|
| `CorsFilter` | 跨域 |
| `JwtAuthenticationFilter` | 解析 Token → 校验签名 → 查 Redis 黑名单 → 注入 SecurityContext + ThreadLocal |

#### Interceptor 层（Controller 前后）
| Interceptor | 职责 |
|---|---|
| `LoginInterceptor` | 检查 ThreadLocal 用户；配合 `@LoginRequired` 注解，方法级精度 |
| `AdminInterceptor` | `/admin-api/**` 路径专用，校验 ADMIN 角色 |
| `OperationLogInterceptor` | 记录管理后台关键操作日志 |
| `ApiTimingInterceptor` | 统计接口耗时，慢接口告警 |

#### AOP 切面（业务横切）
| 切面 | 职责 |
|---|---|
| `SearchHistoryAspect` | 配合 `@SearchHistory` 注解，异步写搜索历史 |
| `RateLimitAspect` | 配合 `@RateLimit` 注解，基于 Redisson `RRateLimiter` 实现令牌桶限流 |
| `CacheableResultAspect` | 配合 `@CacheableResult` 注解，方法结果缓存到 Redis |
| `DistributedLockAspect` | 配合 `@DistLock(key="seckill:{0}")` 注解，基于 Redisson `RLock` 加可重入分布式锁 |

---

## 4. 后端项目结构

### 4.1 多模块 Maven 工程

```
bookstore-parent/                     父项目（packaging=pom）
├── pom.xml                           聚合 + dependencyManagement 统一版本
│
├── bookstore-common/                 通用基础设施（jar）
│   ├── response/   Result, ResultCode, PageResult
│   ├── exception/  BusinessException, AuthException, GlobalExceptionHandler
│   ├── annotation/ @LoginRequired @AdminRequired @RateLimit @SearchHistory @CacheableResult @DistLock
│   ├── aspect/     SearchHistoryAspect, RateLimitAspect, CacheableResultAspect, DistributedLockAspect
│   ├── filter/     CorsFilter, JwtAuthenticationFilter
│   ├── interceptor/ LoginInterceptor, AdminInterceptor, OperationLogInterceptor, ApiTimingInterceptor
│   ├── util/       JwtUtil, PasswordUtil, VectorUtil, JsonUtil, IpUtil
│   ├── context/    UserContext (ThreadLocal)
│   ├── oss/        OssClient, OssConfig, StsService
│   ├── redisson/   RedissonConfig, RedissonHelper（Stream/DelayedQueue 工具封装）
│   └── base/       BaseEntity, BaseController, BaseService
│
├── bookstore-domain/                 领域对象（jar）— 被所有上层依赖
│   ├── entity/     PO, 与 DB 表 1:1
│   ├── dto/        入参对象
│   ├── vo/         出参对象
│   ├── bo/         业务对象（Service 内部流转）
│   ├── query/      查询条件对象
│   ├── enums/      OrderStatus, UserRole, BookStatus, PayStatus, CouponType, CouponStatus, SeckillStatus
│   └── converter/  MapStruct 转换器
│
├── bookstore-mapper/                 数据访问层（jar）
│   ├── *Mapper.java
│   └── resources/mapper/*.xml
│
├── bookstore-service/                业务逻辑层（jar）
│   ├── auth/       AuthService（登录/注册/刷新/登出）
│   ├── user/       UserService
│   ├── book/       BookService, CategoryService
│   ├── cart/, order/, address/, favorite/, review/
│   ├── coupon/     CouponTemplateService, UserCouponService, CouponCalculatorService
│   └── seckill/    SeckillActivityService, SeckillOrderService, SeckillStockService,
│                   SeckillStreamProducer, SeckillStreamConsumer, SeckillTimeoutHandler
│
├── bookstore-ai/                     AI 模块（jar）— 独立，可微服务化
│   ├── client/     DashScopeClient（通义千问 SDK 封装）
│   ├── service/    EmbeddingService, VectorSearchService, RagService, RecommendService
│   └── config/     DashScopeConfig
│
├── bookstore-web-api/                用户端 Controller（jar）
│   └── controller/ AuthController, BookController, CartController, OrderController,
│                   CouponController, SeckillController, ...
│
├── bookstore-admin-api/              管理端 Controller（jar）
│   └── controller/ admin/* 下所有 Controller（含 CouponTemplateController, SeckillActivityController）
│
└── bookstore-app/                    启动模块（executable jar）— 唯一可执行
    ├── BookstoreApplication.java     @SpringBootApplication
    ├── config/    WebMvcConfig, RedisConfig, MybatisPlusConfig, SwaggerConfig
    └── resources/
        ├── application.yml           公共配置
        ├── application-dev.yml       本地
        ├── application-prod.yml      Linux 部署
        ├── logback-spring.xml
        └── db/migration/             Flyway 自动迁移
            ├── V1__init_schema.sql
            ├── V2__seed_categories.sql
            ├── V3__seed_books.sql
            ├── V4__add_book_vector.sql
            ├── V5__seed_user_data.sql
            ├── V6__add_coupon_tables.sql
            ├── V7__add_seckill_tables.sql
            └── V8__seed_coupon_seckill_data.sql
```

### 4.2 模块依赖关系（单向无循环）

```
                  common
                    ▲
                    │
                  domain
        ┌──────────┼──────────┐
        ▼          ▼          ▼
      mapper       │     （domain 直接被 ai/web/admin 引用）
        ▲
        │
      service
        ▲
   ┌────┼─────┐
   │    │     │
   ai web-api admin-api
   ▲    ▲     ▲
   └────┼─────┘
        │
       app  → 产物 bookstore-app.jar
```

### 4.3 对象分层规范

| 层 | 用途 | 命名 | 例子 |
|---|---|---|---|
| **PO** (Entity) | 与表 1:1 | `Xxx`（无后缀） | `Book`, `Order`, `User` |
| **DTO** | 前端 → 后端 | `XxxDTO` | `LoginDTO`, `BookCreateDTO` |
| **VO** | 后端 → 前端 | `XxxVO` | `BookDetailVO`, `OrderListVO` |
| **BO** | Service 间流转 | `XxxBO` | `OrderBO`, `RecommendBO` |
| **Query** | 列表查询条件 | `XxxQuery` | `BookQuery`, `OrderQuery` |

转换交给 MapStruct（编译期生成，无反射），所有 `*Converter.java` 集中在 `domain/converter/`。

### 4.4 可复用组件清单

1. `Result<T>` — 统一响应体
2. `BaseEntity` — id/createTime/updateTime/deleted 公共字段，MyBatis-Plus 自动填充
3. `BaseService` — 继承 IService 提供通用 CRUD
4. `UserContext` — ThreadLocal 取当前用户
5. `@LoginRequired / @AdminRequired` — 方法级鉴权
6. `@RateLimit(qps=5)` — Redisson `RRateLimiter` 令牌桶
7. `@SearchHistory` — 自动异步写搜索历史
8. `@CacheableResult` — 方法结果缓存到 Redis
9. `@DistLock(key="...", waitMs=, leaseMs=)` — Redisson `RLock` 分布式可重入锁
10. `VectorUtil` — 余弦相似度 + Top-K（RAG 与推荐共用）
11. `JwtUtil` — Token 签发/校验/刷新
12. `RedissonHelper` — 封装 `RStream` / `RDelayedQueue` / `RSemaphore` 常用操作

---

## 5. 数据模型

### 5.1 ER 关系图

```
        ┌─────────┐    ┌──────────────┐
        │  user   │───*│   address    │
        │ (含role) │    └──────────────┘
        └────┬────┘
             │ 1
        ┌────┴────────────┬────────────┬──────────────┬─────────────┐
        │ *               │ *          │ *            │ *           │ *
   ┌────▼─────┐    ┌──────▼─────┐  ┌───▼──────┐  ┌────▼──────┐  ┌──▼────────────┐
   │cart_item │    │ favorite   │  │ review   │  │search_hist│  │  order_main   │
   └────┬─────┘    └──────┬─────┘  └────┬─────┘  └───────────┘  └──────┬────────┘
        │                 │             │                              │ 1
        │ *               │ *           │ *                            │
        │                 │             │                              │ *
        │             ┌───▼─────────────▼────┐                  ┌──────▼─────┐
        └────────────>│        book          │<─────────────────│ order_item │
                      │  （含向量 JSON 字段）│                  └────────────┘
                      └──────────┬───────────┘
                                 │ *
                                 │
                            ┌────▼─────┐
                            │ category │（支持父子，二级分类）
                            └──────────┘

   ┌─────────────────┐    ┌──────────────────┐
   │ chat_message    │    │ operation_log    │
   │ (AI 对话历史)   │    │ (管理员操作日志) │
   └─────────────────┘    └──────────────────┘

   ─── 营销模块 ────────────────────────────────────────────────
   ┌──────────────────┐    ┌──────────────┐
   │ coupon_template  │───*│ user_coupon  │  ─── 关联 user, 使用时关联 order_main
   │ (优惠券模板)     │    │ (用户领的券) │
   └──────────────────┘    └──────────────┘

   ┌──────────────────┐    ┌──────────────┐
   │ seckill_activity │───*│ seckill_order│  ─── 关联 user, 关联 book, 落库后归并到 order_main
   │ (秒杀活动)       │    │ (秒杀订单)   │
   └──────────────────┘    └──────────────┘
```

### 5.2 公共字段（所有业务表）

```sql
id           BIGINT      AUTO_INCREMENT PRIMARY KEY
create_time  DATETIME    DEFAULT CURRENT_TIMESTAMP
update_time  DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
deleted      TINYINT(1)  DEFAULT 0      -- 逻辑删除
```

由 `BaseEntity` 统一定义，MyBatis-Plus 自动填充。

### 5.3 表字段定义

#### `user` 用户

| 字段 | 类型 | 约束 |
|---|---|---|
| username | VARCHAR(50) | UNIQUE |
| password | VARCHAR(100) | BCrypt 加密 |
| nickname | VARCHAR(50) | |
| avatar | VARCHAR(255) | OSS 相对 key |
| email | VARCHAR(100) | |
| phone | VARCHAR(20) | INDEX |
| role | VARCHAR(20) | `USER` / `ADMIN` |
| status | VARCHAR(20) | `NORMAL` / `BANNED` |

#### `category` 分类

| 字段 | 类型 | 约束 |
|---|---|---|
| name | VARCHAR(50) | |
| code | VARCHAR(50) | UNIQUE |
| parent_id | BIGINT | 0 = 一级分类 |
| icon_url | VARCHAR(255) | |
| sort_order | INT | |

#### `book` 书籍 ★ 核心表

| 字段 | 类型 | 说明 |
|---|---|---|
| isbn | VARCHAR(20) | UNIQUE |
| title | VARCHAR(200) | FULLTEXT |
| author | VARCHAR(100) | |
| publisher | VARCHAR(100) | |
| publish_date | DATE | |
| category_id | BIGINT | INDEX |
| price | DECIMAL(10,2) | 现价 |
| original_price | DECIMAL(10,2) | 原价（显示折扣） |
| stock | INT | 库存 |
| sales_count | INT | 累计销量，INDEX(status, sales_count DESC) |
| cover_url | VARCHAR(255) | OSS 相对 key |
| description | TEXT | 简介（给 LLM 当上下文） |
| **description_vector** | **JSON** | **1024 维向量** |
| status | VARCHAR(20) | `ON_SHELF` / `OFF_SHELF` |

#### `address` 收货地址

| 字段 | 类型 |
|---|---|
| user_id | BIGINT, INDEX |
| receiver_name | VARCHAR(50) |
| receiver_phone | VARCHAR(20) |
| province / city / district | VARCHAR(50) |
| detail | VARCHAR(255) |
| is_default | TINYINT(1) |

#### `cart_item` 购物车

| 字段 | 类型 | 约束 |
|---|---|---|
| user_id | BIGINT | UNIQUE(user_id, book_id) |
| book_id | BIGINT | |
| quantity | INT | |
| selected | TINYINT(1) | 是否勾选下单 |

#### `order_main` 订单（避免 `order` 关键字）

| 字段 | 类型 | 说明 |
|---|---|---|
| order_no | VARCHAR(32) | UNIQUE |
| user_id | BIGINT | INDEX(user_id, create_time DESC) |
| receiver_name | VARCHAR(50) | 快照 |
| receiver_phone | VARCHAR(20) | 快照 |
| address_snapshot | VARCHAR(500) | 完整地址快照 |
| total_amount | DECIMAL(10,2) | |
| pay_amount | DECIMAL(10,2) | |
| status | VARCHAR(20) | `PENDING_PAY` / `PAID` / `SHIPPED` / `COMPLETED` / `CANCELLED` |
| pay_method | VARCHAR(20) | `MOCK`（本期仅模拟） |
| pay_time | DATETIME | |

#### `order_item` 订单明细

| 字段 | 类型 |
|---|---|
| order_id | BIGINT, INDEX |
| book_id | BIGINT |
| book_title | VARCHAR(200)（快照） |
| book_cover | VARCHAR(255)（快照） |
| unit_price | DECIMAL(10,2)（快照） |
| quantity | INT |
| subtotal | DECIMAL(10,2) |

#### `favorite` 收藏

| 字段 | 类型 | 约束 |
|---|---|---|
| user_id | BIGINT | UNIQUE(user_id, book_id) |
| book_id | BIGINT | |

#### `review` 评价

| 字段 | 类型 | 说明 |
|---|---|---|
| user_id | BIGINT | |
| book_id | BIGINT | INDEX(book_id, create_time DESC) |
| order_id | BIGINT | 关联订单，确保购买后才能评价 |
| rating | TINYINT | 1-5 星 |
| content | VARCHAR(1000) | |
| images | JSON | 图片 OSS key 数组 |

#### `search_history` 搜索历史 ★ 给推荐用

| 字段 | 类型 | 说明 |
|---|---|---|
| user_id | BIGINT | INDEX(user_id, create_time DESC) |
| keyword | VARCHAR(255) | |
| search_type | VARCHAR(20) | `TEXT` / `AI_QUERY` |
| result_count | INT | |

#### `chat_message` AI 对话历史

| 字段 | 类型 | 说明 |
|---|---|---|
| user_id | BIGINT | |
| session_id | VARCHAR(64) | 会话 ID |
| role | VARCHAR(20) | `USER` / `ASSISTANT` |
| content | TEXT | |
| recommended_book_ids | JSON | 该轮推荐的 book id 数组 |

#### `operation_log` 操作日志

| 字段 | 类型 | 说明 |
|---|---|---|
| admin_id | BIGINT | |
| action | VARCHAR(50) | 如 `BOOK_CREATE`, `ORDER_REFUND` |
| target_type | VARCHAR(50) | |
| target_id | BIGINT | |
| params | JSON | 操作参数快照 |
| ip | VARCHAR(50) | |

#### `coupon_template` 优惠券模板 ★ 营销

| 字段 | 类型 | 说明 |
|---|---|---|
| name | VARCHAR(100) | 券名，如"满 100 减 20" |
| type | VARCHAR(20) | `FULL_REDUCE`(满减) / `DISCOUNT`(折扣) / `AMOUNT`(直减) |
| threshold | DECIMAL(10,2) | 使用门槛（满 X 元可用，0 表示无门槛） |
| value | DECIMAL(10,2) | 满减金额 / 折扣率(0.8) / 直减金额 |
| total_count | INT | 发行总量 |
| claimed_count | INT | 已领取数量 |
| valid_from | DATETIME | 生效时间 |
| valid_to | DATETIME | 失效时间 |
| status | VARCHAR(20) | `READY` / `ISSUING` / `ENDED` |

#### `user_coupon` 用户优惠券

| 字段 | 类型 | 说明 |
|---|---|---|
| user_id | BIGINT | INDEX(user_id, status) |
| template_id | BIGINT | INDEX |
| code | VARCHAR(32) | 唯一券码 |
| status | VARCHAR(20) | `UNUSED` / `LOCKED` / `USED` / `EXPIRED` |
| locked_order_no | VARCHAR(32) | 下单时锁定的订单号 |
| used_order_no | VARCHAR(32) | 实际使用的订单号 |
| used_at | DATETIME | 使用时间 |

#### `seckill_activity` 秒杀活动 ★ 核心

| 字段 | 类型 | 说明 |
|---|---|---|
| book_id | BIGINT | INDEX |
| seckill_price | DECIMAL(10,2) | 秒杀价 |
| total_stock | INT | 秒杀总库存 |
| sold_count | INT | 已售数量（DB 乐观锁更新） |
| start_time | DATETIME | INDEX |
| end_time | DATETIME | |
| limit_per_user | INT | 单用户限购数量 |
| status | VARCHAR(20) | `READY`(未开始) / `RUNNING` / `ENDED` |

#### `seckill_order` 秒杀订单（轻量预订）

| 字段 | 类型 | 说明 |
|---|---|---|
| seckill_no | VARCHAR(32) | UNIQUE，前端轮询 key |
| user_id | BIGINT | INDEX(user_id, activity_id) |
| activity_id | BIGINT | UNIQUE(user_id, activity_id) — 限购约束 |
| book_id | BIGINT | |
| status | VARCHAR(20) | `PENDING_PAY` / `PAID` / `CANCELLED` / `TIMEOUT` |
| order_no | VARCHAR(32) | 关联 `order_main.order_no`，支付时同步生成 |
| expire_time | DATETIME | 5 分钟后过期 |

### 5.4 设计取舍说明

1. **角色用 user.role 字段** — 简化登录逻辑，JWT 里带 role 即可
2. **订单金额/地址/商品全部快照** — 任何后续变动不影响历史订单
3. **向量字段存 JSON** — MySQL 8 不支持向量索引，Java 端做 Top-K，数千本以内性能足够
4. **逻辑删除** — `deleted=1` 标记，可追溯，符合企业审计
5. **`seckill_order` 与 `order_main` 拆分** — 秒杀热路径只写轻量 `seckill_order`(快)，支付时再生成正式 `order_main`，互不干扰
6. **`coupon_template.claimed_count` 仅用于统计** — 真正防超发靠 Redisson `RSemaphore`，DB 字段允许有秒级延迟
7. **`user_coupon` 三态(UNUSED/LOCKED/USED)** — 下单结算锁定，支付成功转使用，订单取消解锁 → 防止同一张券并发重用

---

## 6. OSS 集成

### 6.1 存储分工

| 数据 | 位置 |
|---|---|
| 业务数据（用户、订单、书籍、向量、优惠券、秒杀活动） | MySQL 8 |
| Token 黑名单 / 缓存 / 限流 / 会话 / 分布式锁 / 信号量 / Stream MQ / 延时队列 | Redis 7（Redisson 客户端访问） |
| **图片文件**（书封、头像、评价图、Banner） | **阿里云 OSS** |

### 6.2 上传方式 — 前端直传 + STS Token

```
   用户浏览器                    后端 Spring Boot                阿里云 OSS
   ─────────                    ─────────────────              ────────────
   选图片
       │
       ├─① POST /api/oss/sts ────►（鉴权：已登录）
       │                            生成临时 STS 凭证
       │                          （限定 bucket / 路径前缀 / 1h 过期）
       │◄────────── ② STS 凭证 ──────
       │
       ├─③ 直传 PutObject ────────────────────────────────►  OSS 存储
       │◄─────────────── ④ 上传成功，返回 ETag/URL ─────────
       │
       └─⑤ POST /api/books（cover_key=xxx）──►  保存到 DB
```

### 6.3 Bucket 目录规划

```
bookstore-prod/                          (Bucket)
├── books/cover/{yyyy}/{mm}/{uuid}.jpg
├── users/avatar/{userId}/{uuid}.jpg
├── reviews/{reviewId}/{uuid}.jpg
├── banners/home/{uuid}.jpg
└── admin-uploads/{yyyy}/{mm}/{uuid}.{ext}
```

### 6.4 数据库字段约定

DB 中存 OSS 相对 key（不是完整 URL）：

| 字段 | 示例 |
|---|---|
| `book.cover_url` | `books/cover/2026/05/a3f8e1...jpg` |
| `user.avatar` | `users/avatar/123/b9c2d4...jpg` |
| `review.images` (JSON) | `["reviews/45/x.jpg", "reviews/45/y.jpg"]` |

前端读取时拼接 `https://bookstore-prod.oss-cn-hangzhou.aliyuncs.com/{key}` 或 CDN 域名。换 Bucket / 上 CDN 时不需改 DB。

### 6.5 安全要点

1. AccessKey 走环境变量，不入仓库
2. STS 角色权限最小化（仅 `oss:PutObject` 到指定路径前缀）
3. 上传白名单（前后端双验）：仅 `image/jpeg|png|webp`，大小 < 5MB
4. Bucket 配 Referer 白名单防盗链
5. 管理员后台上传增加 `@AdminRequired`

---

## 7. API 设计

### 7.1 全局约定

| 项 | 约定 |
|---|---|
| 前缀 | 用户端 `/api/**`，管理端 `/admin-api/**` |
| 认证 | `Authorization: Bearer <accessToken>` |
| 响应 | `{ code, msg, data }`，成功 `code=200` |
| 分页 | `pageNum`(默认 1) + `pageSize`(默认 20) |
| 时间 | ISO 8601（`2026-05-01T10:30:00`） |
| 错误码 | 200 / 400 / 401 / 403 / 500 / 1xxx 业务错 |
| 文档 | Knife4j（`/doc.html`） |

### 7.2 用户端接口（约 50 个）

#### 认证
| 方法 | 路径 | 鉴权 |
|---|---|---|
| POST | /api/auth/register | 无 |
| POST | /api/auth/login | 无 |
| POST | /api/auth/refresh | 无 |
| POST | /api/auth/logout | 登录 |

#### 用户
| 方法 | 路径 | 鉴权 |
|---|---|---|
| GET | /api/user/profile | 登录 |
| PUT | /api/user/profile | 登录 |
| PUT | /api/user/password | 登录 |
| PUT | /api/user/avatar | 登录 |

#### 书籍
| 方法 | 路径 | 鉴权 |
|---|---|---|
| GET | /api/books | 无（@SearchHistory） |
| GET | /api/books/{id} | 无 |
| GET | /api/books/categories | 无 |
| GET | /api/books/hot | 无 |
| GET | /api/books/new | 无 |

#### 购物车
| 方法 | 路径 | 鉴权 |
|---|---|---|
| GET | /api/cart | 登录 |
| POST | /api/cart | 登录 |
| PUT | /api/cart/{itemId} | 登录 |
| PUT | /api/cart/{itemId}/select | 登录 |
| DELETE | /api/cart/{itemId} | 登录 |
| DELETE | /api/cart | 登录 |

#### 订单
| 方法 | 路径 | 鉴权 |
|---|---|---|
| POST | /api/orders | 登录 |
| GET | /api/orders | 登录 |
| GET | /api/orders/{orderNo} | 登录 |
| POST | /api/orders/{orderNo}/pay | 登录 |
| PUT | /api/orders/{orderNo}/cancel | 登录 |
| PUT | /api/orders/{orderNo}/confirm | 登录 |

#### 收藏
| 方法 | 路径 | 鉴权 |
|---|---|---|
| GET | /api/favorites | 登录 |
| POST | /api/favorites | 登录 |
| DELETE | /api/favorites/{bookId} | 登录 |

#### 评价
| 方法 | 路径 | 鉴权 |
|---|---|---|
| GET | /api/books/{id}/reviews | 无 |
| POST | /api/reviews | 登录 |
| DELETE | /api/reviews/{id} | 登录 |

#### 收货地址
| 方法 | 路径 | 鉴权 |
|---|---|---|
| GET | /api/addresses | 登录 |
| POST | /api/addresses | 登录 |
| PUT | /api/addresses/{id} | 登录 |
| DELETE | /api/addresses/{id} | 登录 |
| PUT | /api/addresses/{id}/default | 登录 |

#### AI
| 方法 | 路径 | 鉴权 | 说明 |
|---|---|---|---|
| POST | /api/ai/chat | 登录 + RateLimit | SSE 流式 |
| GET | /api/ai/chat/sessions | 登录 | |
| GET | /api/ai/chat/sessions/{sessionId} | 登录 | |
| GET | /api/ai/recommend/for-me | 可选登录 | 游客返回热销 |
| GET | /api/ai/recommend/similar/{bookId} | 无 | |

#### 优惠券
| 方法 | 路径 | 鉴权 | 说明 |
|---|---|---|---|
| GET | /api/coupons/available | 无 | 当前可领取的券模板列表 |
| POST | /api/coupons/{templateId}/claim | 登录 + RateLimit | 领券，Redisson 信号量保证不超发 |
| GET | /api/coupons/mine | 登录 | 我的券，按状态筛选 |
| POST | /api/orders/calc-coupon | 登录 | 下单结算时返回最优可用券 |

#### 秒杀
| 方法 | 路径 | 鉴权 | 说明 |
|---|---|---|---|
| GET | /api/seckill/activities | 无 | 进行中 + 即将开始的秒杀活动 |
| GET | /api/seckill/activities/{id} | 无 | 活动详情（含 Redis 实时库存） |
| POST | /api/seckill/activities/{id}/buy | 登录 + RateLimit | 秒杀下单，返回 seckillNo |
| GET | /api/seckill/orders/{seckillNo} | 登录 | 轮询秒杀订单状态 |

#### OSS
| 方法 | 路径 | 鉴权 |
|---|---|---|
| GET | /api/oss/sts | 登录 |

### 7.3 管理端接口

全部需 `@AdminRequired`。

#### 书籍 & 分类
| 方法 | 路径 |
|---|---|
| GET / POST / PUT / DELETE | /admin-api/books |
| PUT | /admin-api/books/{id}/shelf |
| POST | /admin-api/books/{id}/regenerate-vector |
| GET / POST / PUT / DELETE | /admin-api/categories |

#### 订单
| 方法 | 路径 |
|---|---|
| GET | /admin-api/orders |
| GET | /admin-api/orders/{orderNo} |
| PUT | /admin-api/orders/{orderNo}/ship |

#### 用户
| 方法 | 路径 |
|---|---|
| GET | /admin-api/users |
| PUT | /admin-api/users/{id}/status |

#### 优惠券模板
| 方法 | 路径 | 说明 |
|---|---|---|
| GET / POST / PUT / DELETE | /admin-api/coupon-templates | CRUD |
| POST | /admin-api/coupon-templates/{id}/issue | 上架，初始化 Redisson 信号量 |
| PUT | /admin-api/coupon-templates/{id}/end | 提前下架 |

#### 秒杀活动
| 方法 | 路径 | 说明 |
|---|---|---|
| GET / POST / PUT / DELETE | /admin-api/seckill-activities | CRUD |
| PUT | /admin-api/seckill-activities/{id}/start | 提前开启（同步库存到 Redis） |
| PUT | /admin-api/seckill-activities/{id}/end | 提前结束（清理 Redis） |

#### 统计
| 方法 | 路径 | 返回 |
|---|---|---|
| GET | /admin-api/stats/overview | 用户数、新增、总订单、销售额 |
| GET | /admin-api/stats/sales-trend | 30 天销售额趋势 |
| GET | /admin-api/stats/hot-books | Top 10 |
| GET | /admin-api/stats/category-sales | 分类销售占比 |
| GET | /admin-api/stats/ai-usage | AI 调用次数 |
| GET | /admin-api/stats/seckill-conversion | 秒杀转化率（PV → 下单 → 支付） |

#### 操作日志
| 方法 | 路径 |
|---|---|
| GET | /admin-api/logs |

#### 内部维护
| 方法 | 路径 | 说明 |
|---|---|---|
| POST | /admin-api/internal/init-vectors | 一次性批量生成所有上架书籍的 description_vector,首次部署后调用 |

### 7.4 接口数量小结

| 模块 | 接口数 |
|---|---|
| 认证 | 4 |
| 用户 | 4 |
| 书籍 | 5 |
| 购物车 | 6 |
| 订单 | 6 |
| 收藏 | 3 |
| 评价 | 3 |
| 地址 | 5 |
| AI | 5 |
| 优惠券（用户） | 4 |
| 秒杀（用户） | 4 |
| OSS | 1 |
| 管理后台 | 28+ |
| **合计** | **约 78** |

---

## 8. AI 模块详细设计

### 8.1 智能客服（RAG）流程

```
   用户输入
     "想看一本讲商业谈判的实用书，适合上班族，300 页以内"
        │
        ▼
   ChatController（@LoginRequired @RateLimit(qps=1)）
        │ 调 RagService
        ▼
   ① 取多轮历史   chat_message 取本 sessionId 最近 5 条
   ② 改写问题     上下文 + 当前问题 → LLM 改写为独立查询
   ③ 向量化       EmbeddingService.embed(改写后的查询) → 1024 维
   ④ 向量检索     VectorSearchService.topK(query, k=8)
                  从 book 表读 description_vector，Java 端余弦计算 Top 8
   ⑤ 拼 Prompt    System + 8 本书摘要 + 用户问题
   ⑥ 调 qwen-plus 流式调用 DashScope SDK
   ⑦ SSE 推送     一边收增量，一边推前端（打字机效果）
   ⑧ 末尾事件     [event: books] 携带本次推荐的 bookId 列表
   ⑨ 异步落库     chat_message + search_history(AI_QUERY)
```

### 8.2 Prompt 模板

```
[System]
你是"暖意书店"的 AI 阅读顾问"小书"，任务是根据用户描述，从下方书目中精选 3-5 本推荐。

约束：
1. 必须从给定书目中选，不得编造书名或 ISBN
2. 每本书 1-2 句推荐理由，结合书籍特点和用户需求
3. 输出严格按下面 JSON 格式，不要额外文字：
{
  "intro": "一句话开场白",
  "recommendations": [
    {"bookId": <数字>, "reason": "<推荐理由>"},
    ...
  ]
}

[Context]
候选书籍（共 8 本）：
[1] bookId=12  《优势谈判》 罗杰·道森，商业类，250 页，介绍：商业谈判的核心策略...
[2] bookId=45  《沟通的艺术》 罗纳德·阿德勒，沟通类，420 页，介绍：涵盖人际沟通...
[3] ...

[User]
想看一本讲商业谈判的实用书，适合上班族，300 页以内
```

LLM 强制 JSON 输出，后端解析后将 `bookId` 转为完整书籍 VO。解析失败降级为纯文本回复。

### 8.3 多轮对话上下文

- 每对话有 `sessionId`（前端首次会话生成 UUID）
- 第二轮起，先用历史 + 当前问题让 LLM 改写为独立问题（解决代词指向）
  - 例：上一轮"推荐玄幻"，本轮"再推荐几本类似的" → 改写为"推荐和《诡秘之主》类似的玄幻小说"
- 改写后的查询用于向量检索
- 历史超过 10 轮时，只保留最近 5 轮 + 摘要

### 8.4 智能推荐流程

```
   GET /api/ai/recommend/for-me
        │
        ▼
   ① 缓存命中?  Redis: rec:user:{userId}
       命中 → 返回（TTL 1h）
       未命中 → 继续
   ② 查行为数据
       - 最近 20 条 search_history    权重 1.0
       - 最近 10 条 favorite          权重 1.5
       - 最近 10 条 已购买 book        权重 2.0
       都没有 → 冷启动 → 返回热销 Top 10
   ③ 构造用户兴趣向量
       userVec = Σ(weight × embed(行为)) / Σ(weights)
   ④ 向量检索 Top-K
       VectorSearchService.topK(userVec, k=20)
   ⑤ 过滤 & 排序
       - 排除 status != ON_SHELF
       - 排除已购买
       - 排除已收藏
       - 多样性：同分类 ≤ 3 本
       留 Top 10
   ⑥ 写 Redis 1h，返回
```

`/api/ai/recommend/similar/{bookId}` 复用第 ④ 步，直接以该书向量检索。

### 8.5 Embedding 触发场景

| 场景 | 时机 | 调用 |
|---|---|---|
| 书籍新增/导入 | Service 同步 | `embed(title + " " + description)` 写 `description_vector` |
| 书籍描述修改 | Service 同步 | 重新 embed |
| 历史数据初始化 | 启动后一次性脚本 | 批量 embed，单次最多 25 条 |
| 用户行为 | 推荐查询时 | 单条 embed，缓存 5 分钟 |
| 管理员手工 | `POST /admin-api/books/{id}/regenerate-vector` | 单条 |

embedding 调用走异步 + 重试，失败记录到 `embed_retry_queue`（Redis List），定时任务补偿。

### 8.6 向量检索实现

```java
public List<BookSimilarity> topK(double[] queryVec, int k) {
    // 1. 上架书全量 (status=ON_SHELF, deleted=0)
    List<BookVecRow> rows = bookMapper.findAllOnShelfWithVector();
    
    // 2. Java 端余弦相似度
    PriorityQueue<BookSimilarity> heap = new PriorityQueue<>(k);
    for (BookVecRow row : rows) {
        double sim = VectorUtil.cosineSimilarity(queryVec, row.vector);
        if (heap.size() < k) heap.offer(new BookSimilarity(row.id, sim));
        else if (sim > heap.peek().sim) {
            heap.poll();
            heap.offer(new BookSimilarity(row.id, sim));
        }
    }
    return new ArrayList<>(heap).stream()
        .sorted(comparing(BookSimilarity::sim).reversed())
        .toList();
}
```

性能估算：1000 本 × 1024 维 ≈ 100 万次浮点乘加，Java 单核 < 5ms。全量向量缓存到 Caffeine（5 分钟过期）后更快。

### 8.7 缓存策略

| Key | TTL | 用途 |
|---|---|---|
| `rec:user:{userId}` | 1h | 个性化推荐 |
| `rec:similar:{bookId}` | 24h | 相似书籍 |
| `book:vectors:all` | 5m | 全量向量（Caffeine） |
| `embed:keyword:{md5}` | 5m | 关键词 embedding 复用 |
| `chat:session:{sessionId}` | 30m | 当前会话 |
| `ratelimit:ai:{userId}` | 60s | AI 限流计数 |

### 8.8 异常与降级

| 故障 | 降级 |
|---|---|
| 通义 API 超时/限流 | 客服：返回基于关键词全文检索的兜底；推荐：返回热销榜 |
| AccessKey 失效 | 启动校验失败拒绝启动，告警 |
| 向量缺失 | 后台 embed 任务补齐；查询时该书排末尾 |
| Redis 不可用 | 直接走 DB |

---

## 9. 营销模块详细设计（优惠券 + 秒杀）

### 9.1 模块定位

| 模块 | 价值 | 核心技术 |
|---|---|---|
| 优惠券 | 提升复购率、毕设展示营销玩法 | Redisson `RSemaphore` 防超发 + 三态券（UNUSED/LOCKED/USED） |
| 秒杀 | 高并发场景、毕设核心亮点 | Redis `DECR` 预减 + Stream 异步落库 + `RDelayedQueue` 超时回滚 |

两个模块都建立在 **Redisson 一站式分布式协同** 上，无 `synchronized`，热路径全程不进数据库。

### 9.2 优惠券模块

#### 9.2.1 数据流（领券）

```
管理员              用户                        Redis（Redisson）
  │                  │                           │
  │ 创建模板         │                           │
  ├─ INSERT ─────────┤                           │
  │  status=READY    │                           │
  │                  │                           │
  │ 上架 issue       │                           │
  ├──────────────────────────────────────────────►│ RSemaphore.trySetPermits(total)
  │  status=ISSUING  │                           │
  │                  │                           │
  │                  │ ① 浏览可领取             │
  │                  │   GET /api/coupons/available
  │                  │                           │
  │                  │ ② 点击领取               │
  │                  │   POST /api/coupons/{id}/claim
  │                  │   ├── tryAcquire 1 ──────►│ 信号量 -1（无锁原子）
  │                  │   │   fail → "已抢光"     │
  │                  │   ◄──── ok ───────────────│
  │                  │   INSERT user_coupon     │
  │                  │   ASYNC: claimed_count++ │
  │                  │   返回券码               │
```

#### 9.2.2 用券（结算时）

```
   下单结算
     │
     ▼
① 前端选券 / 后端 calc-coupon 推荐最优券
② 校验：归属、状态=UNUSED、未过期、订单金额 ≥ threshold
③ 加 Redisson RLock("user_coupon:{id}")
   ├ status=UNUSED → 改 LOCKED + 写 locked_order_no
   └ status≠UNUSED → 抛 BusinessException("券已使用或锁定")
④ 释放锁
⑤ 创建订单（pay_amount = total - 券折扣）
⑥ 支付成功回调：LOCKED → USED + 写 used_order_no + used_at
⑦ 订单取消：LOCKED → UNUSED（释放）
⑧ 24h 仍是 LOCKED 自动释放（防止下单未付丢券）
```

#### 9.2.3 三种券型计算（CouponCalculatorService）

```java
public BigDecimal calc(BigDecimal orderAmount, CouponTemplate t) {
    if (orderAmount.compareTo(t.threshold) < 0) return ZERO;
    return switch (t.type) {
        case FULL_REDUCE -> t.value;                              // 满 100 减 20
        case AMOUNT      -> t.value;                              // 直减 5 元
        case DISCOUNT    -> orderAmount.multiply(ONE.subtract(t.value));
                                                                  // 8 折(value=0.8) → 减 20%
    };
}
```

最优券推荐：在用户所有 UNUSED 券中遍历，返回 **折扣最大且不超过订单金额** 的那张。

### 9.3 秒杀模块

#### 9.3.1 时间轴与状态机

```
 T-N min       T0(start)             T1(end)
   │              │                     │
   │   READY      │       RUNNING       │      ENDED
   │ (DB only)    │ (Redis 库存激活)    │ (Redis 清理)
   │              │                     │
                  │
       预热定时任务 SeckillStockService.warmup(activityId)：
       ├ Redis SET seckill:stock:{id} = total_stock
       ├ Redis SET seckill:status:{id} = RUNNING
       ├ DB UPDATE status=RUNNING
       └ Redis EXPIRE 至 end_time + 1h
```

#### 9.3.2 秒杀热路径（用户点击 → 立刻返回）

```
POST /api/seckill/activities/{id}/buy   @LoginRequired @RateLimit(qps=5)
   │
   ▼
① 校验 Redis seckill:status:{id} = RUNNING
   ▼
② 限购：Redis SETNX user_seckill:{userId}:{activityId} = 1, EX = 24h
   失败 → "您已抢购过"
   ▼
③ 预减库存：Redis DECR seckill:stock:{activityId}
   < 0 → INCR 还原 + DEL 限购键 + 返回"已售罄"
   ≥ 0 → 继续
   ▼
④ 生成 seckillNo (UUID)
⑤ XADD seckill-stream * userId={u} activityId={a} bookId={b} seckillNo={n}
   ▼
⑥ 立刻返回 { seckillNo, status: "PENDING" }
   ◄──────────── 全程 ≤ 10ms ────────────

前端拿到 seckillNo 后每秒轮询：
GET /api/seckill/orders/{seckillNo}
  → PENDING / PENDING_PAY（可去支付）/ CANCELLED
```

#### 9.3.3 异步消费者（SeckillStreamConsumer）

```java
// 启动时 createGroup("seckill-stream", "seckill-group")
@PostConstruct
void run() {
    while (running) {
        List<MapRecord<String,Object,Object>> records = stream.read(
            Consumer.from("seckill-group", "consumer-1"),
            StreamOffset.create("seckill-stream", ReadOffset.lastConsumed()));
        for (var r : records) {
            try {
                processOne(r);
                stream.acknowledge("seckill-group", r);  // XACK
            } catch (Exception e) {
                log.error("seckill consume fail, retry from PEL", e);
                // 不 ACK → 进 PEL → 下次自动重试
            }
        }
    }
}

@Transactional
void processOne(MapRecord r) {
    // ① DB 乐观锁扣减（最终防线）
    int rows = activityMapper.decrStock(activityId);
    // UPDATE seckill_activity SET sold_count=sold_count+1
    // WHERE id=? AND sold_count<total_stock
    if (rows == 0) throw new BusinessException("库存兜底失败");

    // ② 创建 seckill_order
    seckillOrderMapper.insert(SeckillOrder.builder()
        .seckillNo(...).userId(...).activityId(...).bookId(...)
        .status(PENDING_PAY)
        .expireTime(now().plusMinutes(5))
        .build());

    // ③ 投递 5 分钟延时取消消息
    redisson.getDelayedQueue(timeoutQueue)
        .offer(new SeckillTimeoutEvent(seckillNo), 5, TimeUnit.MINUTES);
}
```

#### 9.3.4 PEL 重试与死信

| 场景 | 处理 |
|---|---|
| 消费异常（DB 抖动等） | 不 ACK → 进 PEL → 下次循环自动重试 |
| 重试 ≥ 3 次 | 移到死信 Stream `seckill-stream-dead` |
| 死信处理 | 定时任务：还原 Redis 库存 + 释放限购键 + 告警 |
| Consumer 宕机 | XCLAIM 抢回宕机 Consumer 的 PEL |

#### 9.3.5 五分钟超时回滚

```
SeckillTimeoutHandler 监听 RDelayedQueue → RBlockingQueue
   │
   ▼
① 取出 SeckillTimeoutEvent(seckillNo)
② 查 seckill_order
   - PENDING_PAY → 继续
   - PAID / CANCELLED → 跳过
③ 加 Redisson RLock("seckill_order:{seckillNo}")
④ UPDATE seckill_order SET status=TIMEOUT
⑤ DB: UPDATE seckill_activity SET sold_count=sold_count-1 WHERE id=?
⑥ Redis: INCR seckill:stock:{activityId}
⑦ Redis: DEL user_seckill:{userId}:{activityId}（释放限购）
⑧ 释放锁
```

#### 9.3.6 防超卖三层兜底

```
   流量 ──► [Redis DECR]            截 99% 流量（内存原子）
              │
              ▼
            [Stream 串行消费]      消费者组保证同 partition 顺序
              │
              ▼
            [DB 乐观锁 UPDATE]     最终防线 sold_count<total_stock
```

任意一层失败 → 已扣 Redis 库存自动 INCR 还原（事务补偿）。

#### 9.3.7 与正式订单融合

```
PENDING_PAY ──pay──► PAID ──同步──► 创建 order_main（快照 + 秒杀价）
      │                                   │
      │ 5 min 未付                        ▼
      ▼                          用户在常规订单列表查看
   TIMEOUT
      │
      ▼ 自动
  库存回滚
```

支付成功时：
1. seckill_order.status = PAID
2. 创建 order_main（order_no 反写到 seckill_order.order_no）
3. 后续退款 / 物流走标准订单流程

### 9.4 Redisson 组件汇总

| 组件 | 用途 | 关键 API |
|---|---|---|
| `RLock` | 用券锁、超时回滚锁、库存兜底锁 | `tryLock(waitMs, leaseMs, MILLISECONDS)` |
| `RSemaphore` | 优惠券总量信号量 | `trySetPermits(N)` / `tryAcquire(1)` |
| `RRateLimiter` | 秒杀按钮单用户限流 | `setRate(OVERALL, 5, 1, SECONDS)` / `tryAcquire(1)` |
| `RAtomicLong` | 实时库存计数（辅助统计） | `incrementAndGet()` |
| `RStream<...>` | 秒杀异步下单 | `add()` / `readGroup()` / `ack()` |
| `RDelayedQueue<...>` | 5 分钟未付自动回滚 | `offer(msg, 5, MINUTES)` |
| `RTopic<...>` | 优惠券广播事件 | `publish()` / `addListener()` |

### 9.5 配置（application.yml 增量）

```yaml
redisson:
  single-server-config:
    address: redis://${REDIS_HOST:localhost}:6379
    password: ${REDIS_PASSWORD:}
    database: 0
    connection-pool-size: 32
    connection-minimum-idle-size: 8
    timeout: 3000
    retry-attempts: 3

seckill:
  stream-name: seckill-stream
  consumer-group: seckill-group
  consumers: 2                      # 同节点并发消费数
  timeout-minutes: 5
  rate-limit-per-user: 5            # 单用户秒级 QPS

coupon:
  user-lock-timeout-hours: 24       # 锁定的券 24h 自动释放
```

### 9.6 异常与降级

| 故障 | 降级 |
|---|---|
| Redis 不可用 | 秒杀活动整体禁用 + 优惠券领取禁用，返回 503 |
| Stream 消费持续失败 | 进死信 + 告警 + 还原库存 |
| Redisson 客户端断连 | 自动重连，断连期间秒杀接口返回 SERVICE_UNAVAILABLE |
| DB 乐观锁失败（极少） | Redis 库存补偿 INCR + 死信记录 + 告警 |

### 9.7 监控指标（Prometheus）

| 指标 | 用途 |
|---|---|
| `seckill_request_total{activity_id}` | 秒杀请求总数 |
| `seckill_decr_fail_total` | 库存预减失败（已售罄） |
| `seckill_consume_fail_total` | Stream 消费失败（PEL） |
| `seckill_dlq_total` | 死信总数 |
| `coupon_claim_total{template_id}` | 领券次数 |
| `coupon_claim_fail_total` | 领券失败（已抢光） |

---

## 10. 前端结构

### 10.1 两个独立项目

| 项目 | 部署 | 技术 |
|---|---|---|
| `bookstore-user-frontend/` | Nginx 站点 1（80） | Vue 3 + Vite + Element Plus + Pinia + Vue Router 4 + Axios |
| `bookstore-admin-frontend/` | Nginx 站点 2（8081） | 同上 + ECharts |

### 10.2 用户端目录

```
bookstore-user-frontend/
├── package.json
├── vite.config.js                 dev 时代理 /api/** → 后端
├── nginx.conf                     生产部署
├── index.html
└── src/
    ├── main.js
    ├── App.vue
    ├── router/index.js            路由 + 全局守卫
    ├── stores/                    Pinia
    │   ├── user.js                token + 当前用户（持久化）
    │   ├── cart.js                购物车数量徽标
    │   └── chat.js                AI 会话 sessionId
    ├── api/
    │   ├── request.js             axios 实例 + 拦截器
    │   ├── sse.js                 SSE 流式接收（fetch 流式）
    │   ├── auth.js / book.js / cart.js / order.js
    │   ├── favorite.js / review.js / address.js
    │   ├── ai.js                  chat / recommend
    │   └── oss.js                 STS + 直传
    ├── views/
    │   ├── login/Login.vue        左右分栏登录
    │   ├── register/Register.vue
    │   ├── home/Home.vue          经典电商首页
    │   ├── book/{BookList, BookDetail}.vue
    │   ├── cart/Cart.vue
    │   ├── order/{OrderConfirm, PaySimulate, OrderList, OrderDetail}.vue
    │   ├── user/{Profile, ChangePassword, Address, Favorite}.vue
    │   └── error/{404, 403}.vue
    ├── components/
    │   ├── layout/{Header, Footer, BasicLayout}.vue
    │   ├── BookCard.vue / BookCover.vue
    │   ├── CategoryNav.vue
    │   ├── ChatWidget.vue         右下角 AI 浮动球 + 弹窗
    │   ├── ChatBubble.vue         打字机渲染
    │   ├── OssUploader.vue        OSS 直传组件
    │   └── PriceTag.vue
    ├── directives/lazyload.js
    ├── utils/{jwt, format, constants}.js
    ├── styles/{main, variables}.css
    └── assets/
```

### 10.3 管理端目录

```
bookstore-admin-frontend/
└── src/
    ├── layout/AdminLayout.vue
    ├── views/
    │   ├── login/Login.vue
    │   ├── dashboard/Dashboard.vue   ECharts 数据看板
    │   ├── book/{BookList, BookEdit}.vue
    │   ├── category/CategoryList.vue
    │   ├── order/{OrderList, OrderDetail}.vue
    │   ├── user/UserList.vue
    │   ├── stats/{SalesTrend, HotBooks, CategorySales}.vue
    │   └── log/OperationLog.vue
    └── api/admin/
```

### 10.4 关键技术片段

#### 路由守卫
```js
router.beforeEach((to) => {
  const u = useUserStore()
  if (to.meta.requireAuth && !u.isLogin) {
    return `/login?redirect=${encodeURIComponent(to.fullPath)}`
  }
})
```

#### Axios 拦截器
```js
const http = axios.create({ baseURL: '/api', timeout: 10000 })

http.interceptors.request.use(cfg => {
  const u = useUserStore()
  if (u.token) cfg.headers.Authorization = `Bearer ${u.token}`
  return cfg
})

http.interceptors.response.use(
  resp => {
    const { code, msg, data } = resp.data
    if (code === 200) return data
    if (code === 401) { /* 尝试 refresh, 失败跳登录 */ }
    ElMessage.error(msg)
    return Promise.reject(new Error(msg))
  }
)
```

#### SSE 流式接收
```js
export async function chatStream({ sessionId, content }, onText, onBooks) {
  const u = useUserStore()
  const resp = await fetch('/api/ai/chat', {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${u.token}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({ sessionId, content })
  })
  const reader = resp.body.getReader()
  const decoder = new TextDecoder()
  let buffer = ''
  while (true) {
    const { value, done } = await reader.read()
    if (done) break
    buffer += decoder.decode(value, { stream: true })
    const events = parseSseEvents(buffer)
    events.forEach(e =>
      e.event === 'books' ? onBooks(e.data) : onText(e.data)
    )
    buffer = events.tail
  }
}
```

#### OSS 直传
```js
import OSS from 'ali-oss'

export async function uploadToOss(file, scene = 'AVATAR') {
  const sts = await http.get('/oss/sts', { params: { scene } })
  const client = new OSS({
    region: sts.region,
    bucket: sts.bucket,
    accessKeyId: sts.accessKeyId,
    accessKeySecret: sts.accessKeySecret,
    stsToken: sts.securityToken,
    secure: true,
  })
  const key = `${sts.pathPrefix}${uuid()}.${getExt(file.name)}`
  await client.put(key, file)
  return key
}
```

### 10.5 主题样式变量

```css
/* styles/variables.css */
:root {
  --primary:        #d68a4f;
  --primary-soft:   #f6a36f;
  --primary-bg:     #fff8f1;
  --secondary-bg:   #fff3e3;
  --border:         #f0e0cc;
  --text-primary:   #5b3a17;
  --text-secondary: #a07a4f;
  --gradient:       linear-gradient(135deg, #f7c89c, #d68a4f);
}
```

Element Plus 主色通过 SCSS 变量覆盖统一应用。

### 10.6 ChatWidget 全局挂载

```html
<!-- App.vue -->
<RouterView />
<ChatWidget />   <!-- 全局，所有页面右下角浮动球 -->
```

未登录点击浮动球弹登录提示。

---

## 11. 部署方案

### 11.1 开发环境准备

| 组件 | 版本 | 安装方式 |
|---|---|---|
| JDK | 17 | Adoptium / Zulu |
| Maven | 3.9+ | IDEA 内置或独立 |
| MySQL | 8.0+ | Docker 推荐 |
| Redis | 7+ | Docker 推荐 |
| Node.js | 20 LTS | nvm |
| Nginx | 1.24+ | 仅生产 |
| 通义 AccessKey | — | DashScope 控制台 |
| 阿里云 OSS | — | RAM 子账号 + STS Role |

#### 本地依赖一键启动
```yaml
# scripts/dev-deps.docker-compose.yml
services:
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: dev123
      MYSQL_DATABASE: bookstore
    ports: ["3306:3306"]
    volumes: ["./mysql-data:/var/lib/mysql"]
  redis:
    image: redis:7-alpine
    ports: ["6379:6379"]
```

### 11.2 本地启动顺序

```
① docker compose -f scripts/dev-deps.docker-compose.yml up -d
② IDEA 启动 BookstoreApplication（profile=dev，环境变量准备好）
③ cd bookstore-user-frontend && npm install && npm run dev → http://localhost:5173
④ cd bookstore-admin-frontend && npm install && npm run dev → http://localhost:5174
⑤ POST http://localhost:8080/admin-api/internal/init-vectors （一次性生成向量）
```

### 11.3 配置分层

#### `application.yml`（公共）
```yaml
spring:
  application:
    name: bookstore
  profiles:
    active: ${PROFILE:dev}
  jpa:
    open-in-view: false
mybatis-plus:
  global-config:
    db-config:
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0
oss:
  endpoint: ${OSS_ENDPOINT}
  bucket: ${OSS_BUCKET}
  access-key-id: ${OSS_AK}
  access-key-secret: ${OSS_SK}
ai:
  dashscope:
    api-key: ${DASHSCOPE_API_KEY}
    chat-model: qwen-plus
    embedding-model: text-embedding-v3
    timeout: 30s
jwt:
  secret: ${JWT_SECRET}
  access-token-ttl: 2h
  refresh-token-ttl: 7d
```

#### `application-dev.yml`
```yaml
server:
  port: 8080
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/bookstore
    username: root
    password: dev123
  redis:
    host: localhost
    port: 6379
logging:
  level:
    com.bookstore: DEBUG
```

#### `application-prod.yml`
```yaml
server:
  port: 8080
  tomcat:
    threads:
      max: 200
spring:
  datasource:
    url: jdbc:mysql://${DB_HOST}:3306/bookstore?useSSL=true
    username: ${DB_USER}
    password: ${DB_PASSWORD}
    hikari:
      maximum-pool-size: 20
  redis:
    host: ${REDIS_HOST}
    port: 6379
    password: ${REDIS_PASSWORD}
logging:
  level:
    com.bookstore: INFO
  file:
    path: /var/log/bookstore
```

敏感信息全部走环境变量。

### 11.4 Linux 上线步骤

#### Step 1 · 服务器准备
```bash
sudo useradd -m -s /bin/bash bookstore
sudo mkdir -p /opt/bookstore /var/log/bookstore
sudo chown -R bookstore:bookstore /opt/bookstore /var/log/bookstore
```

#### Step 2 · 安装运行时
```bash
sudo apt install openjdk-17-jdk -y
sudo apt install mysql-server -y
sudo mysql_secure_installation
# CREATE DATABASE bookstore CHARACTER SET utf8mb4;
# CREATE USER 'bookstore'@'%' IDENTIFIED BY '强密码';
# GRANT ALL ON bookstore.* TO 'bookstore'@'%';

sudo apt install redis-server -y
# 设 requirepass、bind 内网

sudo apt install nginx -y
```

#### Step 3 · 后端打包部署
```bash
mvn clean package -DskipTests
scp bookstore-app/target/bookstore-app.jar bookstore@server:/opt/bookstore/
```

systemd 服务 `/etc/systemd/system/bookstore.service`：
```ini
[Unit]
Description=Bookstore Backend
After=mysql.service redis.service network.target

[Service]
Type=simple
User=bookstore
WorkingDirectory=/opt/bookstore
EnvironmentFile=/opt/bookstore/app.env
ExecStart=/usr/bin/java -Xms512m -Xmx1g \
          -Dspring.profiles.active=prod \
          -jar /opt/bookstore/bookstore-app.jar
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
```

`/opt/bookstore/app.env`（权限 600）：
```
DB_HOST=127.0.0.1
DB_USER=bookstore
DB_PASSWORD=...
REDIS_HOST=127.0.0.1
REDIS_PASSWORD=...
JWT_SECRET=随机64位
DASHSCOPE_API_KEY=...
OSS_ENDPOINT=https://oss-cn-hangzhou.aliyuncs.com
OSS_BUCKET=bookstore-prod
OSS_AK=...
OSS_SK=...
```

```bash
sudo systemctl daemon-reload
sudo systemctl enable --now bookstore
journalctl -u bookstore -f
```

#### Step 4 · 前端打包部署
```bash
cd bookstore-user-frontend && npm run build
cd ../bookstore-admin-frontend && npm run build

scp -r bookstore-user-frontend/dist/* bookstore@server:/var/www/bookstore-user/
scp -r bookstore-admin-frontend/dist/* bookstore@server:/var/www/bookstore-admin/
```

Nginx 配置 `/etc/nginx/conf.d/bookstore.conf`：
```nginx
server {
  listen 80;
  server_name bookstore.example.com;
  root /var/www/bookstore-user;
  index index.html;

  location / {
    try_files $uri $uri/ /index.html;     # SPA history 模式
  }

  location /api/ {
    proxy_pass http://127.0.0.1:8080;
    proxy_http_version 1.1;
    proxy_set_header Connection '';        # SSE 必需
    proxy_buffering off;                   # SSE 必需
    proxy_read_timeout 600s;
  }
}

server {
  listen 8081;
  root /var/www/bookstore-admin;
  index index.html;

  location / { try_files $uri $uri/ /index.html; }
  location /admin-api/ {
    proxy_pass http://127.0.0.1:8080;
  }
}
```

```bash
sudo nginx -t
sudo systemctl reload nginx
```

#### Step 5 · 防火墙 / HTTPS
```bash
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw allow 8081/tcp     # 建议加 IP 白名单

sudo apt install certbot python3-certbot-nginx -y
sudo certbot --nginx -d bookstore.example.com
```

### 11.5 监控 & 运维

| 项 | 工具 / 路径 |
|---|---|
| 应用日志 | `/var/log/bookstore/*.log`（按日切割，保留 30 天） |
| systemd 日志 | `journalctl -u bookstore` |
| Nginx 日志 | `/var/log/nginx/access.log`, `error.log` |
| 健康检查 | `curl http://127.0.0.1:8080/actuator/health` |
| Prometheus 指标 | `/actuator/prometheus`（可对接 Grafana） |
| MySQL 备份 | crontab `mysqldump bookstore > /backup/bookstore-$(date +%F).sql` |
| OSS 备份 | OSS 自带版本控制 + 跨区复制 |

### 11.6 部署 Checklist

- [ ] `application-prod.yml` 不在仓库，全走环境变量
- [ ] JWT_SECRET 重新生成
- [ ] MySQL 业务账号最小权限
- [ ] Redis 设密码 + 仅监听 127.0.0.1
- [ ] OSS RAM 子账号最小权限 + STS Role
- [ ] 防火墙只开必须端口
- [ ] HTTPS 证书 + 强 TLS
- [ ] 管理端 IP 白名单或登录失败次数限制
- [ ] 日志路径有写权限 + 磁盘充足
- [ ] systemd 服务 enable
- [ ] crontab 数据库备份
- [ ] Knife4j 生产环境关闭

### 11.7 升级流程
```bash
mvn clean package -DskipTests
scp target/bookstore-app.jar bookstore@server:/opt/bookstore/bookstore-app.jar.new
sudo systemctl stop bookstore
sudo mv /opt/bookstore/bookstore-app.jar /opt/bookstore/bookstore-app.jar.bak
sudo mv /opt/bookstore/bookstore-app.jar.new /opt/bookstore/bookstore-app.jar
sudo systemctl start bookstore
curl http://127.0.0.1:8080/actuator/health
```

Flyway 启动时自动应用新增 `V*.sql`。

---

## 12. 测试与验收

### 12.1 测试数据

#### 分类（10 个一级分类）

| 分类 | 书目数 | 例子 |
|---|---|---|
| 文学 | 8 | 《活着》《围城》《百年孤独》《追风筝的人》《白夜行》《了不起的盖茨比》《红楼梦》《平凡的世界》 |
| 玄幻 | 8 | 《诡秘之主》《斗破苍穹》《盘龙》《全职法师》《吞噬星空》《我欲封天》《择天记》《雪中悍刀行》 |
| 大学教材 | 10 | 《高等数学》(同济)《线性代数》《概率论与数理统计》《数据结构》(严蔚敏)《计算机网络》(谢希仁)《操作系统》(汤子瀛)《编译原理》《数据库系统概论》《大学物理》《C 程序设计》(谭浩强) |
| 科幻 | 6 | 《三体》三部曲《球状闪电》《沙丘》《银河帝国》 |
| 历史 | 6 | 《人类简史》《明朝那些事儿》《史记》《万历十五年》《枪炮、病菌与钢铁》《全球通史》 |
| 童书 | 5 | 《小王子》《窗边的小豆豆》《夏洛的网》《纳尼亚传奇》《哈利·波特》 |
| 艺术 | 4 | 《艺术的故事》《写给大家的中国美术史》《设计中的设计》《色彩心理学》 |
| 心理 | 5 | 《心理学与生活》《被讨厌的勇气》《自卑与超越》《非暴力沟通》《思考，快与慢》 |
| 商业 | 5 | 《原则》《精益创业》《从 0 到 1》《增长黑客》《优势谈判》 |
| 计算机 | 7 | 《算法导论》《Effective Java》《深入理解计算机系统》《代码大全》《设计模式》《重构》《图解 HTTP》 |
| **合计** | **64** | |

每本书：title, author, publisher, isbn, price, original_price, stock(50-200 随机), description(80-150 字), cover_url(预上传 OSS)。

#### 测试用户

| 用户名 | 密码 | 角色 | 预置数据 |
|---|---|---|---|
| `admin` | `admin123` | ADMIN | — |
| `alice` | `123456` | USER | 收货地址 2 条、收藏 5 本、搜索历史 8 条、已完成订单 2 条（可写评价）、已领优惠券 2 张（1 张 UNUSED、1 张 USED） |
| `bob` | `123456` | USER | 收货地址 1 条、购物车 3 项、待支付订单 1 条、已领优惠券 1 张（演示下单选券） |
| `charlie` | `123456` | USER | 全新用户（演示冷启动推荐 + 首次领券） |

预置订单/评价/搜索历史在 `V5__seed_user_data.sql` 中插入。

#### 优惠券模板（V8 种子）

| 名称 | 类型 | 门槛 | 面值 | 总量 | 已领 | 有效期 | 状态 |
|---|---|---|---|---|---|---|---|
| 新人立减 20 | FULL_REDUCE | 100 元 | 20 元 | 1000 | 50 | 长期 90 天 | ISSUING |
| 满 200 减 50 | FULL_REDUCE | 200 元 | 50 元 | 500 | 30 | 长期 60 天 | ISSUING |
| 全场 9 折 | DISCOUNT | 0 元 | 0.10（10% 折扣） | 200 | 10 | 长期 30 天 | ISSUING |
| 答辩演示券（10 元无门槛） | AMOUNT | 0 元 | 10 元 | 100 | 0 | 长期 7 天 | ISSUING |

#### 秒杀活动（V8 种子）

| 活动书籍 | 原价 | 秒杀价 | 总库存 | 已售 | 单用户限购 | 时间 | 状态 |
|---|---|---|---|---|---|---|---|
| 《三体》 | 59 | 19.9 | 100 | 0 | 1 | 演示当日 20:00–22:00 | READY |
| 《算法导论》 | 128 | 49 | 50 | 0 | 1 | 演示当日 20:00–22:00 | READY |
| 《活着》 | 39 | 9.9 | 200 | 0 | 2 | 长期演示 / 当前 RUNNING | RUNNING |

> 答辩前一天用脚本把"演示当日"调成现在 ± 30 分钟，方便现场触发整段流程；`《活着》` 长期 `RUNNING` 让评委随时打开页面就能看到秒杀按钮。

预置向量：启动后调用 `POST /admin-api/internal/init-vectors` 一次性生成 64 本书的 description_vector（约 30 秒）。

预置 Redis：项目首次启动会自动把活动 `total_stock` 写入 `seckill:stock:{activityId}`、把活动状态写入 `seckill:status:{activityId}`，无需手工执行。

### 12.2 测试策略

| 类型 | 工具 | 覆盖 |
|---|---|---|
| 单元测试 | JUnit 5 + Mockito | Service 核心逻辑（订单计算、向量相似度、Token、密码加密、优惠券计算器、秒杀消费者） |
| 集成测试 | Spring Boot Test + Testcontainers (MySQL + Redis) | 关键 API 端到端（注册→登录→下单→支付→评价、领券→下单选券→支付占用→取消归还、秒杀下单→延时未支付回滚） |
| 接口测试 | Postman Collection | 接口全跑，导出入仓库 `docs/postman/` |
| 压力测试 | JMeter | 商品列表 100 QPS，AI 接口 5 QPS 不超时；**秒杀单接口 500 并发瞬时拍下，库存零超卖**（用 `seckill:stock = 100` 验证最终售出 = 100） |
| 前端 e2e（可选） | Playwright | 登录 + 下单 + 秒杀三条主链路 |

### 12.3 功能验收清单

#### 用户端
- [ ] 注册校验（用户名重复、密码强度）
- [ ] 登录成功跳转、失败提示
- [ ] Token 过期自动刷新
- [ ] 首页 Banner、推荐区、分类入口
- [ ] 书籍列表：分类筛选、关键词搜索、排序（销量/价格/新书）
- [ ] 详情页：简介、评价、相似推荐
- [ ] 购物车：加减、勾选、全选、删除、合计
- [ ] 下单：选地址、确认、模拟支付、跳订单详情
- [ ] 收藏：加/取消，收藏列表
- [ ] 评价：仅有完成订单的书可评、上传图片
- [ ] 多收货地址：CRUD、设默认
- [ ] AI 客服：浮动球弹出、流式打字机、推荐书卡片可点
- [ ] 个性化推荐：基于历史，新用户走热销榜
- [ ] 优惠券：可领、领取上限校验、可在下单页选择最优券、支付后状态变 USED、未支付取消后归还 UNUSED
- [ ] 秒杀活动：列表/详情显示倒计时、未开始按钮置灰、库存归零按钮显示"已抢光"、单用户超过限购拒绝、5 分钟未支付库存自动归还

#### 管理端
- [ ] 管理员独立登录
- [ ] 书籍 CRUD + 上下架 + OSS 上传封面 + 重新生成向量
- [ ] 分类 CRUD
- [ ] 订单列表 + 发货
- [ ] 用户管理 + 封禁/解封
- [ ] 优惠券模板 CRUD + 上架触发 `RSemaphore` 初始化 + 已发数实时刷新
- [ ] 秒杀活动 CRUD + 上架触发 Redis 库存预热 + 转化漏斗看板
- [ ] 数据看板：总览、销售曲线、Top10、分类占比、AI 调用统计、秒杀转化漏斗
- [ ] 操作日志可查

#### 非功能
- [ ] 关键接口 P95 < 500ms（商品列表 < 200ms）
- [ ] AI 接口首字延迟 < 1s
- [ ] 100 并发用户首页加载正常
- [ ] **秒杀下单接口 P99 < 50ms（无 DB 写）**
- [ ] **500 并发抢 100 件商品，最终售出严格等于 100，无超卖**
- [ ] 管理端 UI 在 1366×768 及以上分辨率显示正常
- [ ] 移动端用户端基本可用

---

## 13. 项目时间表（12 周方案）

| 周次 | 主要工作 |
|---|---|
| W1 | 环境准备 + 多模块 Maven 脚手架 + Flyway + 全局响应/异常 + 拦截器/Filter |
| W2 | 用户认证 + JWT + Redis 黑名单 + 用户模块 + 收货地址 + 单元测试规范 |
| W3 | 分类 + 书籍 CRUD + 列表/详情/搜索 + OSS STS 接口 |
| W4 | 购物车 + 收藏夹 + 评价（含图片上传） + 前端基础布局 |
| W5 | 订单流程 + 模拟支付 + 订单状态机 + 前端下单链路 |
| W6 | AI 模块基础设施：DashScope SDK 封装 + Embedding + 向量存储/检索 + Caffeine |
| W7 | ★ 智能客服 RAG：Prompt 工程 + 多轮对话 + SSE 流式 + 前端打字机 |
| W8 | ★ 智能推荐：用户画像 + 加权多源 + 多样性过滤 + 缓存策略 |
| W9 | ★ Redisson 基础设施 + 优惠券模块：模板 CRUD、领取（`RSemaphore` 防超发）、下单选券（三态机：UNUSED→LOCKED→USED）、计算器单元测试 |
| W10 | ★ 秒杀模块：Redis 库存预热、`SETNX` 限购、Stream Producer/Consumer、`RDelayedQueue` 5 分钟超时回滚、PEL 死信、500 并发压测验证零超卖 |
| W11 | 管理后台：CRUD + 优惠券/秒杀活动后台 + 数据看板 ECharts + 秒杀转化漏斗 + 操作日志 |
| W12 | 整体联调 + 单元/集成测试 + Linux 部署 + 论文写作 + 答辩 PPT |

每周末 git commit + push，关键节点打 tag（`v0.1-auth-ready`, `v0.5-mvp`, `v1.0-release`）。

---

## 14. 文档与交付物

| 交付物 | 位置 |
|---|---|
| 设计文档（本文件） | `docs/superpowers/specs/2026-05-01-bookstore-design.md` |
| 论文 .docx | `docs/智能书店系统-毕设论文.docx` |
| ER 图 / UML 类图 | `docs/diagrams/`（PlantUML 或 drawio） |
| 接口文档 | Knife4j 自动生成 + 导出 PDF |
| 数据库脚本 | `bookstore-app/src/main/resources/db/migration/V*.sql` |
| Postman Collection | `docs/postman/bookstore.json` |
| 答辩 PPT | `docs/答辩PPT.pptx` |
| 演示视频（可选） | 5 分钟录屏，主流程 + AI 模块 |
| 部署手册 | `docs/deploy.md`（第 11 章整理） |

---

## 15. 答辩讲解锚点

1. 多模块 Maven 工程，8 个子模块，职责单一，AI 独立可微服务化
2. 对象分层 PO/DTO/VO/BO/Query，MapStruct 编译期生成，无反射开销
3. Flyway 数据库版本管理，自动迁移与回溯
4. JWT 双 Token + Redis 黑名单，登出立即失效
5. RAG 检索增强生成：通义千问 + text-embedding-v3，1024 维向量
6. 多轮对话上下文改写，解决代词指向
7. Prompt 强制 JSON 输出，工程可靠性提升 10 倍
8. SSE 流式返回 + 打字机效果，首字延迟 < 1s
9. 加权多源用户画像（搜索/收藏/购买不同权重） + 余弦 Top-K
10. OSS 前端直传 + STS 临时凭证，大文件不走后端
11. Filter + Interceptor + AOP 三层拦截，职责清晰
12. ECharts 数据可视化，管理后台业务洞察
13. **Redisson 一站式分布式协同**：分布式锁 / 信号量 / 限流器 / 延时队列 / Stream 客户端，整套高并发能力一个 SDK 解决
14. **Redis Stream 消息队列**：Consumer Group + ACK + PEL + 死信 Stream + XCLAIM 故障转移，零新增中间件
15. **秒杀热路径无 synchronized**：`DECR` 内存原子预减 + Stream 异步落库 + 数据库乐观锁兜底（三层防超卖），单接口 P99 < 50ms，500 并发压测零超卖
16. **`RDelayedQueue` 5 分钟超时自动回滚**：未支付订单到点自动归还库存与优惠券，无需轮询扫表
17. **`RSemaphore` 防优惠券超发**：原子许可池替代显式锁，简洁高性能
18. **优惠券三态机**：UNUSED → LOCKED（下单占用）→ USED（支付完成）/ 取消归还，配合 `RLock("user_coupon:{id}")` 防双花

---

## 16. 后续迭代方向（毕设外）

- 真实支付渠道接入（支付宝沙箱/微信支付）
- 物流追踪
- 会员体系 / 积分商城
- 直播带货 / 短视频书籍介绍
- 服务端 SSR（SEO 友好）
- 微服务化（AI 模块独立部署）
- ElasticSearch 全文检索升级
- Milvus / Pinecone 向量数据库（书籍量级超过万本时）
- 国际化 / 多语言
- 消息队列升级到 RocketMQ / Kafka（订单量超过 10 万/日时）

---

**文档版本历史**

| 版本 | 日期 | 修改 |
|---|---|---|
| v1.0 | 2026-05-01 | 初版 |
| v1.1 | 2026-05-01 | 加入营销模块：Redisson + Redis Stream + 优惠券（三态机）+ 秒杀（无 synchronized 热路径 + 延时回滚 + 三层防超卖）；时间表扩展为 12 周 |
