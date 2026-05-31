# Bookstore 公共 API 模块抽取设计

## 背景

当前 bookstore-springcloud 微服务项目中，Feign Client 接口和共享 DTO 分散定义在各业务服务的 `client/` 包下，存在重复编码问题：

- `BookServiceClient` 在 `user-service` 和 `trade-service` 中重复定义
- `UserServiceClient` 在 `book-service` 和 `trade-service` 中重复定义
- 跨服务引用的 DTO（如 `BookDetailVO`、`UserProfileVO`）被多处复制

本设计通过抽取公共 `bookstore-api` 模块解决上述问题，同时为 OpenFeign 启用 OkHttp 连接池优化。

---

## 架构设计

### 模块结构

```
bookstore-springcloud
├── pom.xml                              -- 根 POM
├── bookstore-api                        -- 【新建】公共 API 模块（非 Boot 应用）
│   ├── pom.xml
│   └── src/main/java/com/bookstore/api
│       ├── book/
│       │   ├── client/BookClient.java
│       │   ├── dto/BookDetailDTO.java
│       │   └── fallback/BookClientFallbackFactory.java
│       ├── user/
│       │   ├── client/UserClient.java
│       │   ├── dto/UserProfileDTO.java
│       │   └── fallback/UserClientFallbackFactory.java
│       └── trade/
│           ├── client/TradeClient.java
│           ├── dto/OrderDetailDTO.java
│           └── fallback/TradeClientFallbackFactory.java
├── bookstore-common                     -- 保留（工具类、异常、注解、Result 等）
├── bookstore-gateway                    -- 已有，基本不动
├── bookstore-user-service               -- 删除本地 client/，依赖 api 模块
├── bookstore-book-service               -- 删除本地 client/，依赖 api 模块
├── bookstore-trade-service              -- 删除本地 client/，依赖 api 模块
├── bookstore-ai-service
└── bookstore-admin-service
```

### 依赖关系

```
bookstore-api          -- 纯接口+DTO，依赖 openfeign + okhttp
    ^
bookstore-common       -- 工具类、Result、异常等
    ^
各业务服务 (user/book/trade/ai/admin)
    ^
bookstore-gateway      -- 不依赖 api 模块
```

- `bookstore-api` **不是** Spring Boot 可运行应用，`pom.xml` 不加 `spring-boot-maven-plugin`
- `bookstore-api` 需要 `spring-cloud-starter-openfeign` + `feign-okhttp` + `spring-cloud-starter-loadbalancer`
- 各业务服务同时依赖 `bookstore-common` 和 `bookstore-api`

---

## Feign + OkHttp 连接池配置

当前各服务使用 JDK `HttpURLConnection`（无连接池）。改造后使用 OkHttp：

### 根 POM dependencyManagement

```xml
<dependency>
    <groupId>io.github.openfeign</groupId>
    <artifactId>feign-okhttp</artifactId>
    <version>${feign.version}</version>
</dependency>
```

### api 模块和各服务 POM

```xml
<dependency>
    <groupId>io.github.openfeign</groupId>
    <artifactId>feign-okhttp</artifactId>
</dependency>
```

### 各服务 application.yml

```yaml
feign:
  okhttp:
    enabled: true
  client:
    config:
      default:
        connectTimeout: 5000
        readTimeout: 10000
```

---

## Fallback 降级设计

为每个 Feign Client 配置 FallbackFactory，远程调用失败时返回降级结果：

```java
@Component
@Slf4j
public class BookClientFallbackFactory implements FallbackFactory<BookClient> {
    @Override
    public BookClient create(Throwable cause) {
        log.error("调用 book-service 失败", cause);
        return id -> Result.fail("商品服务暂不可用");
    }
}
```

```java
@FeignClient(name = "book-service", fallbackFactory = BookClientFallbackFactory.class)
public interface BookClient { ... }
```

开启熔断：

```yaml
feign:
  circuitbreaker:
    enabled: true
```

---

## 包扫描处理

现有各服务启动类已声明：

```java
@EnableFeignClients(basePackages = "com.bookstore")
@ComponentScan(basePackages = "com.bookstore")
```

`bookstore-api` 的包路径规划为 `com.bookstore.api.{book,user,trade}`，**完全落在 `com.bookstore` 扫描范围内**，因此：

- **启动类无需修改**
- 不需要额外声明 `@EnableFeignClients(clients = ...)` 或调整 `basePackages`

---

## 具体改造清单

| 步骤 | 操作 |
|---|---|
| 1 | 新建 `bookstore-api` 模块，配置为非 Boot 的 Maven 模块 |
| 2 | 迁移 Feign Client：将各服务的 `client/` 包下接口移到 `bookstore-api/com/bookstore/api/{service}/client/` |
| 3 | 迁移共享 DTO：将跨服务引用的 VO 类移到 `bookstore-api/com/bookstore/api/{service}/dto/`，统一命名为 `*DTO` |
| 4 | 新增 FallbackFactory 实现类到各 `fallback/` 包 |
| 5 | 根 POM `<modules>` 加入 `bookstore-api` |
| 6 | 各业务服务 POM 加入 `bookstore-api` 依赖，删除本地 `client/` 包 |
| 7 | 各服务 `application.yml` 加 `feign.okhttp.enabled: true` |
| 8 | 编译验证所有 import 路径和包扫描正确 |

---

## 待迁移的接口与 DTO

### 当前分散在各服务中的 Feign Client

| Client | 当前所在服务 | 调用目标 | 需迁移到 |
|---|---|---|---|
| `UserServiceClient` | book-service, trade-service | user-service | `api/user/client/UserClient.java` |
| `TradeServiceClient` | book-service | trade-service | `api/trade/client/TradeClient.java` |
| `BookServiceClient` | user-service, trade-service | book-service | `api/book/client/BookClient.java` |

### 当前被跨服务引用的 DTO

| DTO | 当前所在服务 | 被哪些服务引用 | 需迁移到 |
|---|---|---|---|
| `BookDetailVO` | book-service | user-service, trade-service | `api/book/dto/BookDetailDTO.java` |
| `UserProfileVO` | user-service | book-service, trade-service | `api/user/dto/UserProfileDTO.java` |
| `OrderDetailVO` | trade-service | book-service | `api/trade/dto/OrderDetailDTO.java` |
| `AddressVO` | user-service | trade-service | `api/user/dto/AddressDTO.java` |

---

## 风险与注意事项

1. **DTO 迁移后的 import 路径变更**：原服务中的 Controller、Service 引用这些 DTO 的 `import` 语句需要更新为新包路径
2. **API 模块不能依赖业务服务**：`bookstore-api` 只能依赖 `bookstore-common`，不能反向依赖各业务服务，避免循环依赖
3. **Gateway 模块不依赖 api**：Gateway 只做路由转发，不调用 Feign Client
