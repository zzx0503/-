# 智能书店系统 — 微服务版 (Bookstore Microservices)

毕设项目 — Spring Cloud Alibaba 微服务架构。

## 技术栈

- Spring Boot + Spring Cloud Alibaba
- Nacos（注册中心 + 配置中心）
- Spring Cloud Gateway（网关）
- OpenFeign（服务间调用）
- Sentinel（限流熔断）
- Seata（分布式事务）
- MyBatis-Plus
- Redis
- RabbitMQ
- JWT

## 模块说明

```
bookstore-springcloud/
├── bookstore-gateway          # 网关 — 统一入口、路由转发、JWT 校验
├── bookstore-admin-service    # 管理后台服务 — 图书/分类/订单/优惠券管理
├── bookstore-ai-service       # AI 服务 — 智能对话、搜索、推荐
├── bookstore-book-service     # 图书服务 — 图书信息、分类、搜索
├── bookstore-trade-service    # 交易服务 — 购物车、订单、支付、秒杀
├── bookstore-user-service     # 用户服务 — 用户、地址、收藏、签到
├── bookstore-common           # 公共模块 — 工具类、常量、异常
└── bookstore-api              # API 模块 — Feign 客户端、共享 DTO
```

## 快速开始

### 1. 启动基础设施

```bash
# Nacos
startup.cmd -m standalone

# Redis
redis-server

# RabbitMQ
rabbitmq-plugins enable rabbitmq_management
```

### 2. 配置本地密钥

**不要在 `application.yml` 中写真实密钥。**

在各服务的 `src/main/resources/` 下创建 `application-local.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/bookstore?serverTimezone=Asia/Shanghai
    username: root
    password: YOUR_DB_PASSWORD
  data:
    redis:
      host: localhost
      port: 6379
      password: YOUR_REDIS_PASSWORD
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest

bookstore:
  oss:
    access-key-id: YOUR_ACCESS_KEY_ID
    access-key-secret: YOUR_ACCESS_KEY_SECRET
  ai:
    api-key: YOUR_AI_API_KEY
    search-api-key: YOUR_SEARCH_API_KEY
    recommend-api-key: YOUR_RECOMMEND_API_KEY

jwt:
  secret: YOUR_JWT_SECRET
```

启动时激活 local profile：
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

### 3. 编译启动

```bash
# 编译公共模块
mvn clean install -pl bookstore-common,bookstore-api

# 启动各服务（分别在各自目录执行）
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

## 服务端口

| 服务 | 端口 |
|------|------|
| Gateway | 8080 |
| Admin Service | 8081 |
| AI Service | 8082 |
| Book Service | 8083 |
| Trade Service | 8084 |
| User Service | 8085 |

## 前端

前端页面在 [`frontend`](https://github.com/zzx0503/-/tree/frontend) 分支。

## 注意事项

- 各服务的 `application.yml` 中密钥为占位符，**请勿直接修改并提交**
- 真实配置统一放在 `application-local.yml`（已加入 `.gitignore`）
- 生产环境建议使用 Nacos 配置中心或环境变量注入
