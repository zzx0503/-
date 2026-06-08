# 智能书店系统 (Bookstore)

毕设项目 — Spring Boot 3.2.5 单体后端，涵盖图书商城、秒杀、AI 智能搜索/推荐、消息队列等完整功能。

## 技术栈

| 类别 | 技术 |
|------|------|
| 框架 | Spring Boot 3.2.5 + Spring MVC |
| ORM | MyBatis-Plus 3.5.5 |
| 数据库 | MySQL 8.0 + Flyway 迁移 |
| 缓存 | Redis (Lettuce) |
| 分布式 | Redisson 3.27.2（锁/布隆过滤器） |
| 消息队列 | RabbitMQ + Spring AMQP |
| 认证 | JWT (jjwt 0.12.5) 双 token + Redis 黑名单 |
| 对象存储 | 阿里云 OSS（STS 临时凭证） |
| AI | 阿里百炼 DashScope (OpenAI 兼容) + 通义千问 |
| API 文档 | Knife4j 4.4.0 |
| 工具 | Lombok + MapStruct |
| 测试 | JUnit 5 + Testcontainers |

## 功能模块

### 用户
- 注册/登录/登出、JWT 双 token 刷新
- 个人资料修改、密码修改、头像上传（OSS）
- 钱包余额、交易流水

### 图书商城
- 图书分类（二级树）
- 图书列表/详情、多维度搜索
- 购物车 CRUD
- 下单（含优惠券计算）、微信支付模拟
- 收藏夹、搜索历史

### AI 智能助手
- **对话**：多轮对话，支持图书推荐、购物咨询
- **搜索**：自然语言搜书（AI 提取关键词 → 数据库多维检索 → AI 排序精选）
- **推荐**：基于用户画像的个性化推荐

### 秒杀
- 后台管理秒杀活动
- Redis 预售库存 + Lua 原子扣减
- RabbitMQ 异步排队削峰
- 用户限购、订单超时取消

### 优惠券
- 后台创建优惠券模板（满减/折扣/运费券）
- 用户领取、下单时自动计算最优方案
- 锁定/释放/过期处理

### 签到
- 每日签到领金币
- 连续签到奖励递增

### 后台管理
- 仪表盘统计
- 图书/分类 CRUD、封面批量导入
- 订单管理
- 操作日志

## 快速开始

```bash
# 1. 复制配置模板并填入真实值
cp src/main/resources/application-template.yml src/main/resources/application.yml

# 2. 编译
mvn clean compile

# 3. 启动（需 MySQL、Redis、RabbitMQ）
mvn spring-boot:run
```

应用默认监听 `http://localhost:8080`，API 文档 `/doc.html`。

## 外部依赖

| 服务 | 说明 |
|------|------|
| MySQL 8.0 | 主数据库 |
| Redis | 缓存、Token 黑名单、秒杀库存 |
| RabbitMQ | 秒杀队列、订单超时延迟消息 |
| 阿里云 OSS | 头像/封面图片存储 |
| 阿里百炼 DashScope | AI 对话/搜索/推荐 |

## 项目结构

```
bookstore
├── src/main/java/com/bookstore
│   ├── anno             注解 (@LoginRequired, @AdminRequired, @RateLimit)
│   ├── aop              切面 (限流、操作日志、搜索历史)
│   ├── app              启动类 + 框架配置
│   ├── config           业务配置 (OSS, AI, Redisson, MQ, 异步)
│   ├── context          用户上下文 (ThreadLocal)
│   ├── controller       用户端控制器 (16 个) + admin 控制器 (9 个)
│   ├── domain           实体(PO) / DTO / VO
│   ├── exception        全局异常处理
│   ├── filter           过滤器 (JWT 鉴权、CORS)
│   ├── interceptor      拦截器 (登录/管理员)
│   ├── mapper           MyBatis-Plus Mapper
│   ├── response         统一响应 (Result / PageResult / ResultCode)
│   ├── service          业务接口 + impl 实现
│   ├── service/ai       AI 客户端 (OpenAI 兼容协议)
│   └── utils            工具类 (JWT, 密码加密, OSS URL)
├── src/main/resources
│   ├── db/migration     Flyway 迁移脚本 (V1-V11)
│   └── application-template.yml  配置模板
└── src/test             单元测试 + 集成测试
```

## 数据库迁移

使用 Flyway 管理表结构，迁移脚本位于 `src/main/resources/db/migration/`：

| 版本 | 内容 |
|------|------|
| V1 | 初始表结构 (user, book, order, cart, address 等) |
| V2 | 分类与交易扩展 |
| V3 | 图书种子数据 |
| V4 | 钱包与余额 |
| V5 | TXT 图书导入 |
| V6 | 优惠券 |
| V7 | 秒杀 |
| V8 | 操作日志 |
| V9 | AI 对话 |
| V10 | 分类数据更新 |
| V11 | 签到 |

## 配置说明

1. 复制 `application-template.yml` → `application.yml`
2. 填入数据库、Redis、RabbitMQ 连接信息
3. 填入阿里云 OSS AccessKey 和 DashScope API Key
4. `application.yml` 已在 `.gitignore` 中排除，不会被提交
