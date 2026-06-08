# 智能书店系统(Bookstore)

毕设项目 — Spring Boot 3.2.5 + MyBatis-Plus + JWT + Redis 后端。

## 已完成 (v0.1-auth-ready)

- 单模块 Maven 工程,Spring Boot 3.2.5 + Java 17
- Flyway V1 完整 schema(user/address/book/order/...,Plan 2-6 会扩展)
- 三层异常体系:`BusinessException` / `AuthException` / `GlobalExceptionHandler`
- JWT 双 token + Redis 黑名单(jti + TTL)
- 用户认证:注册 / 登录 / 刷新 / 登出
- 用户资料:GET/PUT `/api/user/me`,改密,换头像
- 收货地址:CRUD + 默认切换 + 删默认自动提升
- Testcontainers 集成测试(MySQL + Redis)— `SmokeIT` + `AuthFlowIT`(9 测)
- 13 个端点 Postman collection

## 目录结构

```
bookstore (jar)
└── src/main/java/com/bookstore
    ├── anno        注解(LoginRequired / AdminRequired / RateLimit)
    ├── aop         切面(RateLimitAspect)
    ├── app         启动类 + Spring 配置
    ├── config      额外配置(OpenApiConfig / RedissonConfig)
    ├── context     用户上下文(CurrentUser / UserContext)
    ├── controller  REST 控制器
    ├── domain      PO / DTO / VO / 枚举
    ├── exception   异常体系
    ├── filter      过滤器(CorsFilter / JwtAuthenticationFilter)
    ├── interceptor 拦截器(LoginInterceptor / AdminInterceptor)
    ├── mapper      MyBatis-Plus Mapper
    ├── response    统一响应(Result / PageResult / ResultCode)
    ├── service     业务逻辑接口
    ├── service/impl 业务逻辑实现
    └── utils       工具类
```

## 快速开始

```bash
# 编译 + 单元/切片测试(无需 Docker)
mvn clean test

# 集成测试 (Testcontainers — 需要 Docker)
mvn verify

# 启动应用
mvn spring-boot:run
```

应用默认监听 `http://localhost:8080`。

> 集成测试在 `src/test/java/com/bookstore/app/it/`,文件名后缀 `*IT.java`,由 maven-failsafe-plugin 在 `verify` 阶段执行。需要本机或远程 Docker;远程时设 `DOCKER_HOST=tcp://<vm-ip>:2375`(或 `:2376` + TLS)。

## API 列表 (Plan 1)

### Auth

| Method | Path | 鉴权 | 说明 |
|---|---|---|---|
| POST | /api/auth/register | 否 | 注册并自动登录 |
| POST | /api/auth/login | 否 | 用户名/手机号 + 密码 |
| POST | /api/auth/refresh | 否 | 用 refresh 换新对 |
| POST | /api/auth/logout | 是 | 把 access token 加黑名单 |

### User

| Method | Path | 鉴权 | 说明 |
|---|---|---|---|
| GET | /api/user/me | 是 | 当前用户资料 |
| PUT | /api/user/me | 是 | 改昵称/性别/生日 |
| PUT | /api/user/me/password | 是 | 改密码 |
| PUT | /api/user/me/avatar | 是 | 更新头像 OSS key |

### Address

| Method | Path | 鉴权 | 说明 |
|---|---|---|---|
| GET | /api/address | 是 | 列表(默认在前) |
| POST | /api/address | 是 | 新增 |
| PUT | /api/address/{id} | 是 | 修改 |
| DELETE | /api/address/{id} | 是 | 软删 |
| PUT | /api/address/{id}/default | 是 | 设默认 |

完整 Postman collection 在 [`docs/postman/bookstore-auth.postman_collection.json`](docs/postman/bookstore-auth.postman_collection.json)。
导入后先调 `Auth/Register` 或 `Auth/Login`,test 脚本会自动把 `accessToken` / `refreshToken` 写入 collection variables,后续请求会自动带 `Authorization: Bearer {{accessToken}}`。
