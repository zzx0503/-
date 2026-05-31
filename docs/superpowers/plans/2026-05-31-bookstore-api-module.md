# Bookstore 公共 API 模块抽取 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 抽取 `bookstore-api` 公共模块，将分散在各服务中的 Feign Client 和共享 DTO 统一迁移，并为 OpenFeign 启用 OkHttp 连接池。

**Architecture:** 新建非 Boot 的 `bookstore-api` Maven 模块，按服务分包（`api/book`, `api/user`, `api/trade`），包含 Feign Client 接口、共享 DTO 和 FallbackFactory。各业务服务依赖 api 模块，删除本地重复的 `client/` 包和 VO 类。

**Tech Stack:** Spring Cloud OpenFeign, OkHttp, Spring Cloud Alibaba Nacos, Spring Cloud Circuit Breaker

---

## File Structure

### New Files

| File | Purpose |
|---|---|
| `bookstore-api/pom.xml` | api 模块的 Maven 配置，非 Boot 应用 |
| `bookstore-api/src/main/java/com/bookstore/api/book/client/BookClient.java` | 调用 book-service 的 Feign 接口 |
| `bookstore-api/src/main/java/com/bookstore/api/book/dto/BookDetailDTO.java` | 商品详情 DTO（原 BookDetailVO） |
| `bookstore-api/src/main/java/com/bookstore/api/book/dto/BookListDTO.java` | 商品列表 DTO（原 BookListVO） |
| `bookstore-api/src/main/java/com/bookstore/api/book/fallback/BookClientFallbackFactory.java` | BookClient 降级工厂 |
| `bookstore-api/src/main/java/com/bookstore/api/user/client/UserClient.java` | 调用 user-service 的 Feign 接口 |
| `bookstore-api/src/main/java/com/bookstore/api/user/dto/UserProfileDTO.java` | 用户信息 DTO（原 UserProfileVO） |
| `bookstore-api/src/main/java/com/bookstore/api/user/dto/AddressDTO.java` | 地址 DTO（原 AddressVO） |
| `bookstore-api/src/main/java/com/bookstore/api/user/fallback/UserClientFallbackFactory.java` | UserClient 降级工厂 |
| `bookstore-api/src/main/java/com/bookstore/api/trade/client/TradeClient.java` | 调用 trade-service 的 Feign 接口 |
| `bookstore-api/src/main/java/com/bookstore/api/trade/dto/OrderDetailDTO.java` | 订单详情 DTO（原 OrderDetailVO） |
| `bookstore-api/src/main/java/com/bookstore/api/trade/dto/OrderItemDTO.java` | 订单项 DTO（原 OrderItemVO） |
| `bookstore-api/src/main/java/com/bookstore/api/trade/fallback/TradeClientFallbackFactory.java` | TradeClient 降级工厂 |

### Modified Files

| File | Change |
|---|---|
| `pom.xml` (root) | 新增 `bookstore-api` module，新增 `feign-okhttp` 版本管理 |
| `bookstore-book-service/pom.xml` | 新增 `bookstore-api` 依赖 |
| `bookstore-user-service/pom.xml` | 新增 `bookstore-api` 依赖 |
| `bookstore-trade-service/pom.xml` | 新增 `bookstore-api` 依赖 |
| `bookstore-book-service/src/main/resources/application.yml` | 新增 `feign.okhttp.enabled: true` |
| `bookstore-user-service/src/main/resources/application.yml` | 新增 `feign.okhttp.enabled: true` |
| `bookstore-trade-service/src/main/resources/application.yml` | 新增 `feign.okhttp.enabled: true` |

### Deleted Files

| File | Reason |
|---|---|
| `bookstore-book-service/src/main/java/com/bookstore/client/*.java` | 迁移到 api 模块 |
| `bookstore-user-service/src/main/java/com/bookstore/client/*.java` | 迁移到 api 模块 |
| `bookstore-trade-service/src/main/java/com/bookstore/client/*.java` | 迁移到 api 模块 |
| 各服务 `domain/vo/book/BookDetailVO.java` | 迁移到 api 模块（保留源头服务一份，改 import） |
| 各服务 `domain/vo/book/BookListVO.java` | 迁移到 api 模块 |
| 各服务 `domain/vo/user/UserProfileVO.java` | 迁移到 api 模块 |
| 各服务 `domain/vo/address/AddressVO.java` | 迁移到 api 模块 |
| 各服务 `domain/vo/order/OrderDetailVO.java` | 迁移到 api 模块 |
| 各服务 `domain/vo/order/OrderItemVO.java` | 迁移到 api 模块 |

### Import Path Changes (in existing service files)

以下文件中的 `import com.bookstore.domain.vo.xxx.XXXVO` 需要改为 `import com.bookstore.api.xxx.dto.XXXDTO`：

**book-service:**
- `controller/BookController.java`
- `controller/InternalBookController.java`
- `service/BookService.java`
- `service/impl/BookServiceImpl.java`

**user-service:**
- `controller/UserController.java`
- `controller/InternalUserController.java`
- `service/UserService.java`
- `service/impl/UserServiceImpl.java`
- `controller/AddressController.java`
- `service/AddressService.java`
- `service/impl/AddressServiceImpl.java`

**trade-service:**
- `controller/OrderController.java`
- `controller/InternalOrderController.java`
- `service/OrderService.java`
- `service/impl/OrderServiceImpl.java`

---

## Tasks

### Task 1: 新建 bookstore-api 模块

**Files:**
- Create: `bookstore-api/pom.xml`

- [ ] **Step 1: 创建模块目录和 pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.bookstore</groupId>
        <artifactId>bookstore-springcloud</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>

    <artifactId>bookstore-api</artifactId>
    <packaging>jar</packaging>
    <name>bookstore-api</name>
    <description>公共 API 模块 - Feign Client 和共享 DTO</description>

    <dependencies>
        <!-- 依赖 common 以使用 Result 等通用类 -->
        <dependency>
            <groupId>com.bookstore</groupId>
            <artifactId>bookstore-common</artifactId>
        </dependency>
        <!-- OpenFeign -->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-openfeign</artifactId>
        </dependency>
        <!-- OkHttp -->
        <dependency>
            <groupId>io.github.openfeign</groupId>
            <artifactId>feign-okhttp</artifactId>
        </dependency>
        <!-- LoadBalancer -->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-loadbalancer</artifactId>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 2: 创建目录结构**

Run:
```bash
mkdir -p bookstore-api/src/main/java/com/bookstore/api/{book,user,trade}/{client,dto,fallback}
```

- [ ] **Step 3: Commit**

```bash
git add bookstore-api/
git commit -m "feat: add bookstore-api module"
```

---

### Task 2: 迁移 Book API

**Files:**
- Create: `bookstore-api/src/main/java/com/bookstore/api/book/dto/BookDetailDTO.java`
- Create: `bookstore-api/src/main/java/com/bookstore/api/book/dto/BookListDTO.java`
- Create: `bookstore-api/src/main/java/com/bookstore/api/book/client/BookClient.java`
- Create: `bookstore-api/src/main/java/com/bookstore/api/book/fallback/BookClientFallbackFactory.java`

- [ ] **Step 1: 创建 BookDetailDTO**

```java
package com.bookstore.api.book.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class BookDetailDTO {

    private Long id;
    private String isbn;
    private String title;
    private String subtitle;
    private String author;
    private String translator;
    private String publisher;
    private Long categoryId;
    private String categoryName;
    private String coverKey;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private Integer stock;
    private Integer salesCount;
    private BigDecimal rating;
    private String description;
    private LocalDate publishDate;
    private Integer status;
    private Integer deleted;
    private Boolean isFavorited;
    private List<BookListDTO> relatedBooks;
}
```

- [ ] **Step 2: 创建 BookListDTO**

```java
package com.bookstore.api.book.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class BookListDTO {

    private Long id;
    private String title;
    private String subtitle;
    private String author;
    private String coverKey;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private Integer salesCount;
    private BigDecimal rating;
    private Long categoryId;
    private String categoryName;
}
```

- [ ] **Step 3: 创建 BookClient**

```java
package com.bookstore.api.book.client;

import com.bookstore.api.book.dto.BookDetailDTO;
import com.bookstore.api.book.fallback.BookClientFallbackFactory;
import com.bookstore.response.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "book-service", fallbackFactory = BookClientFallbackFactory.class)
public interface BookClient {

    @GetMapping("/api/internal/book/{id}")
    Result<BookDetailDTO> getBook(@PathVariable("id") Long id);

    @PostMapping("/api/internal/book/{id}/deduct-stock")
    Result<Void> deductStock(@PathVariable("id") Long id, @RequestParam("quantity") Integer quantity);

    @PostMapping("/api/internal/book/{id}/restore-stock")
    Result<Void> restoreStock(@PathVariable("id") Long id, @RequestParam("quantity") Integer quantity);
}
```

- [ ] **Step 4: 创建 BookClientFallbackFactory**

```java
package com.bookstore.api.book.fallback;

import com.bookstore.api.book.client.BookClient;
import com.bookstore.api.book.dto.BookDetailDTO;
import com.bookstore.response.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class BookClientFallbackFactory implements FallbackFactory<BookClient> {

    @Override
    public BookClient create(Throwable cause) {
        log.error("调用 book-service 失败", cause);
        return new BookClient() {
            @Override
            public Result<BookDetailDTO> getBook(Long id) {
                return Result.fail(null, "商品服务暂不可用");
            }

            @Override
            public Result<Void> deductStock(Long id, Integer quantity) {
                return Result.fail(null, "商品服务暂不可用");
            }

            @Override
            public Result<Void> restoreStock(Long id, Integer quantity) {
                return Result.fail(null, "商品服务暂不可用");
            }
        };
    }
}
```

- [ ] **Step 5: Commit**

```bash
git add bookstore-api/src/main/java/com/bookstore/api/book/
git commit -m "feat(api): migrate book API client, DTO and fallback"
```

---

### Task 3: 迁移 User API

**Files:**
- Create: `bookstore-api/src/main/java/com/bookstore/api/user/dto/UserProfileDTO.java`
- Create: `bookstore-api/src/main/java/com/bookstore/api/user/dto/AddressDTO.java`
- Create: `bookstore-api/src/main/java/com/bookstore/api/user/client/UserClient.java`
- Create: `bookstore-api/src/main/java/com/bookstore/api/user/fallback/UserClientFallbackFactory.java`

- [ ] **Step 1: 创建 UserProfileDTO**

```java
package com.bookstore.api.user.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class UserProfileDTO {

    private Long id;
    private String username;
    private String phone;
    private String email;
    private String nickname;
    private String avatarUrl;
    private Integer gender;
    private LocalDate birthday;
    private String role;
}
```

- [ ] **Step 2: 创建 AddressDTO**

```java
package com.bookstore.api.user.dto;

import lombok.Data;

@Data
public class AddressDTO {

    private Long id;
    private String receiver;
    private String phone;
    private String province;
    private String city;
    private String district;
    private String detailAddress;
    private Boolean isDefault;
    private String fullAddress;
}
```

- [ ] **Step 3: 创建 UserClient**

```java
package com.bookstore.api.user.client;

import com.bookstore.api.user.dto.AddressDTO;
import com.bookstore.api.user.dto.UserProfileDTO;
import com.bookstore.api.user.fallback.UserClientFallbackFactory;
import com.bookstore.response.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;

@FeignClient(name = "user-service", fallbackFactory = UserClientFallbackFactory.class)
public interface UserClient {

    @GetMapping("/api/internal/user/{id}")
    Result<UserProfileDTO> getUser(@PathVariable("id") Long id);

    @GetMapping("/api/internal/address/{id}")
    Result<AddressDTO> getAddress(@PathVariable("id") Long id);

    @PostMapping("/api/internal/wallet/pay")
    Result<Void> pay(@RequestParam("userId") Long userId,
                     @RequestParam("orderNo") String orderNo,
                     @RequestParam("amount") BigDecimal amount);
}
```

- [ ] **Step 4: 创建 UserClientFallbackFactory**

```java
package com.bookstore.api.user.fallback;

import com.bookstore.api.user.client.UserClient;
import com.bookstore.api.user.dto.AddressDTO;
import com.bookstore.api.user.dto.UserProfileDTO;
import com.bookstore.response.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@Slf4j
public class UserClientFallbackFactory implements FallbackFactory<UserClient> {

    @Override
    public UserClient create(Throwable cause) {
        log.error("调用 user-service 失败", cause);
        return new UserClient() {
            @Override
            public Result<UserProfileDTO> getUser(Long id) {
                return Result.fail(null, "用户服务暂不可用");
            }

            @Override
            public Result<AddressDTO> getAddress(Long id) {
                return Result.fail(null, "用户服务暂不可用");
            }

            @Override
            public Result<Void> pay(Long userId, String orderNo, BigDecimal amount) {
                return Result.fail(null, "用户服务暂不可用");
            }
        };
    }
}
```

- [ ] **Step 5: Commit**

```bash
git add bookstore-api/src/main/java/com/bookstore/api/user/
git commit -m "feat(api): migrate user API client, DTO and fallback"
```

---

### Task 4: 迁移 Trade API

**Files:**
- Create: `bookstore-api/src/main/java/com/bookstore/api/trade/dto/OrderDetailDTO.java`
- Create: `bookstore-api/src/main/java/com/bookstore/api/trade/dto/OrderItemDTO.java`
- Create: `bookstore-api/src/main/java/com/bookstore/api/trade/client/TradeClient.java`
- Create: `bookstore-api/src/main/java/com/bookstore/api/trade/fallback/TradeClientFallbackFactory.java`

- [ ] **Step 1: 创建 OrderItemDTO**

```java
package com.bookstore.api.trade.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderItemDTO {

    private Long id;
    private Long bookId;
    private String bookTitle;
    private String bookCover;
    private BigDecimal unitPrice;
    private Integer quantity;
    private BigDecimal subtotal;
}
```

- [ ] **Step 2: 创建 OrderDetailDTO**

```java
package com.bookstore.api.trade.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrderDetailDTO {

    private Long id;
    private Long userId;
    private String orderNo;
    private BigDecimal totalAmount;
    private BigDecimal payAmount;
    private BigDecimal discountAmount;
    private Long couponId;
    private String payMethod;
    private LocalDateTime payTime;
    private String status;
    private String remark;
    private String addressSnapshot;
    private LocalDateTime shipTime;
    private LocalDateTime completeTime;
    private LocalDateTime createTime;
    private List<OrderItemDTO> items;
}
```

- [ ] **Step 3: 创建 TradeClient**

```java
package com.bookstore.api.trade.client;

import com.bookstore.api.trade.dto.OrderDetailDTO;
import com.bookstore.api.trade.fallback.TradeClientFallbackFactory;
import com.bookstore.response.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "trade-service", fallbackFactory = TradeClientFallbackFactory.class)
public interface TradeClient {

    @GetMapping("/api/internal/order/{orderNo}")
    Result<OrderDetailDTO> getOrder(@PathVariable("orderNo") String orderNo);

    @GetMapping("/api/internal/order/by-id/{id}")
    Result<OrderDetailDTO> getOrderById(@PathVariable("id") Long id);
}
```

- [ ] **Step 4: 创建 TradeClientFallbackFactory**

```java
package com.bookstore.api.trade.fallback;

import com.bookstore.api.trade.client.TradeClient;
import com.bookstore.api.trade.dto.OrderDetailDTO;
import com.bookstore.response.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class TradeClientFallbackFactory implements FallbackFactory<TradeClient> {

    @Override
    public TradeClient create(Throwable cause) {
        log.error("调用 trade-service 失败", cause);
        return new TradeClient() {
            @Override
            public Result<OrderDetailDTO> getOrder(String orderNo) {
                return Result.fail(null, "交易服务暂不可用");
            }

            @Override
            public Result<OrderDetailDTO> getOrderById(Long id) {
                return Result.fail(null, "交易服务暂不可用");
            }
        };
    }
}
```

- [ ] **Step 5: Commit**

```bash
git add bookstore-api/src/main/java/com/bookstore/api/trade/
git commit -m "feat(api): migrate trade API client, DTO and fallback"
```

---

### Task 5: 根 POM 配置

**Files:**
- Modify: `pom.xml`

- [ ] **Step 1: 在 modules 中加入 bookstore-api**

在 `pom.xml` 的 `<modules>` 中，`bookstore-common` 之后加入：

```xml
<module>bookstore-api</module>
```

- [ ] **Step 2: 在 dependencyManagement 中加入 feign-okhttp**

在 `pom.xml` 的 `<dependencyManagement><dependencies>` 中，Spring Cloud Alibaba 之后加入：

```xml
<dependency>
    <groupId>io.github.openfeign</groupId>
    <artifactId>feign-okhttp</artifactId>
    <version>${feign.version}</version>
</dependency>
```

注意：`${feign.version}` 由 Spring Cloud BOM 管理，如果已隐式管理则此步可省。若编译时报版本找不到，可在 `<properties>` 中显式声明：

```xml
<feign.version>13.2.1</feign.version>
```

- [ ] **Step 3: Commit**

```bash
git add pom.xml
git commit -m "build: add bookstore-api module and feign-okhttp version"
```

---

### Task 6: Book Service 改造

**Files:**
- Modify: `bookstore-book-service/pom.xml`
- Modify: `bookstore-book-service/src/main/resources/application.yml`
- Delete: `bookstore-book-service/src/main/java/com/bookstore/client/TradeServiceClient.java`
- Delete: `bookstore-book-service/src/main/java/com/bookstore/client/UserServiceClient.java`
- Delete: `bookstore-book-service/src/main/java/com/bookstore/domain/vo/user/UserProfileVO.java`
- Delete: `bookstore-book-service/src/main/java/com/bookstore/domain/vo/order/OrderDetailVO.java`
- Delete: `bookstore-book-service/src/main/java/com/bookstore/domain/vo/order/OrderItemVO.java`
- Modify: `bookstore-book-service/src/main/java/com/bookstore/controller/BookController.java`
- Modify: `bookstore-book-service/src/main/java/com/bookstore/controller/InternalBookController.java`
- Modify: `bookstore-book-service/src/main/java/com/bookstore/service/BookService.java`
- Modify: `bookstore-book-service/src/main/java/com/bookstore/service/impl/BookServiceImpl.java`

- [ ] **Step 1: POM 加入 api 依赖**

在 `bookstore-book-service/pom.xml` 的 `<dependencies>` 中，`bookstore-common` 之后加入：

```xml
<dependency>
    <groupId>com.bookstore</groupId>
    <artifactId>bookstore-api</artifactId>
</dependency>
```

- [ ] **Step 2: application.yml 启用 OkHttp**

在 `bookstore-book-service/src/main/resources/application.yml` 的 `jwt:` 同级位置加入：

```yaml
feign:
  okhttp:
    enabled: true
  client:
    config:
      default:
        connectTimeout: 5000
        readTimeout: 10000
  circuitbreaker:
    enabled: true
```

- [ ] **Step 3: 删除本地 client 和冗余 VO**

```bash
rm bookstore-book-service/src/main/java/com/bookstore/client/TradeServiceClient.java
rm bookstore-book-service/src/main/java/com/bookstore/client/UserServiceClient.java
rm bookstore-book-service/src/main/java/com/bookstore/domain/vo/user/UserProfileVO.java
rm bookstore-book-service/src/main/java/com/bookstore/domain/vo/order/OrderDetailVO.java
rm bookstore-book-service/src/main/java/com/bookstore/domain/vo/order/OrderItemVO.java
```

- [ ] **Step 4: 修改 BookController.java 的 import**

将 `import com.bookstore.domain.vo.book.BookDetailVO;` 改为：

```java
import com.bookstore.api.book.dto.BookDetailDTO;
```

同时把方法返回类型中的 `BookDetailVO` 改为 `BookDetailDTO`，并把 `BookListVO` 改为 `BookListDTO`。

类似地，修改 `InternalBookController.java`、`BookService.java`、`BookServiceImpl.java` 中的相关 import 和类型引用。

- [ ] **Step 5: Commit**

```bash
git add bookstore-book-service/
git commit -m "refactor(book-service): use bookstore-api, enable okhttp"
```

---

### Task 7: User Service 改造

**Files:**
- Modify: `bookstore-user-service/pom.xml`
- Modify: `bookstore-user-service/src/main/resources/application.yml`
- Delete: `bookstore-user-service/src/main/java/com/bookstore/client/BookServiceClient.java`
- Delete: `bookstore-user-service/src/main/java/com/bookstore/domain/vo/book/BookDetailVO.java`
- Delete: `bookstore-user-service/src/main/java/com/bookstore/domain/vo/book/BookListVO.java`
- Delete: `bookstore-user-service/src/main/java/com/bookstore/domain/vo/order/OrderDetailVO.java`
- Delete: `bookstore-user-service/src/main/java/com/bookstore/domain/vo/order/OrderItemVO.java`
- Modify: `bookstore-user-service/src/main/java/com/bookstore/controller/UserController.java`
- Modify: `bookstore-user-service/src/main/java/com/bookstore/controller/InternalUserController.java`
- Modify: `bookstore-user-service/src/main/java/com/bookstore/controller/AddressController.java`
- Modify: `bookstore-user-service/src/main/java/com/bookstore/service/UserService.java`
- Modify: `bookstore-user-service/src/main/java/com/bookstore/service/impl/UserServiceImpl.java`
- Modify: `bookstore-user-service/src/main/java/com/bookstore/service/AddressService.java`
- Modify: `bookstore-user-service/src/main/java/com/bookstore/service/impl/AddressServiceImpl.java`

- [ ] **Step 1: POM 加入 api 依赖**

在 `bookstore-user-service/pom.xml` 的 `<dependencies>` 中，`bookstore-common` 之后加入：

```xml
<dependency>
    <groupId>com.bookstore</groupId>
    <artifactId>bookstore-api</artifactId>
</dependency>
```

- [ ] **Step 2: application.yml 启用 OkHttp**

在 `bookstore-user-service/src/main/resources/application.yml` 的 `jwt:` 同级位置加入：

```yaml
feign:
  okhttp:
    enabled: true
  client:
    config:
      default:
        connectTimeout: 5000
        readTimeout: 10000
  circuitbreaker:
    enabled: true
```

- [ ] **Step 3: 删除本地 client 和冗余 VO**

```bash
rm bookstore-user-service/src/main/java/com/bookstore/client/BookServiceClient.java
rm bookstore-user-service/src/main/java/com/bookstore/domain/vo/book/BookDetailVO.java
rm bookstore-user-service/src/main/java/com/bookstore/domain/vo/book/BookListVO.java
rm bookstore-user-service/src/main/java/com/bookstore/domain/vo/order/OrderDetailVO.java
rm bookstore-user-service/src/main/java/com/bookstore/domain/vo/order/OrderItemVO.java
```

- [ ] **Step 4: 修改 import 和类型引用**

将以下文件中的 `import com.bookstore.domain.vo.user.UserProfileVO;` 改为 `import com.bookstore.api.user.dto.UserProfileDTO;`，并把 `UserProfileVO` 改为 `UserProfileDTO`：
- `UserController.java`
- `InternalUserController.java`
- `UserService.java`
- `UserServiceImpl.java`

将以下文件中的 `import com.bookstore.domain.vo.address.AddressVO;` 改为 `import com.bookstore.api.user.dto.AddressDTO;`，并把 `AddressVO` 改为 `AddressDTO`：
- `AddressController.java`
- `AddressService.java`
- `AddressServiceImpl.java`

- [ ] **Step 5: Commit**

```bash
git add bookstore-user-service/
git commit -m "refactor(user-service): use bookstore-api, enable okhttp"
```

---

### Task 8: Trade Service 改造

**Files:**
- Modify: `bookstore-trade-service/pom.xml`
- Modify: `bookstore-trade-service/src/main/resources/application.yml`
- Delete: `bookstore-trade-service/src/main/java/com/bookstore/client/BookServiceClient.java`
- Delete: `bookstore-trade-service/src/main/java/com/bookstore/client/UserServiceClient.java`
- Delete: `bookstore-trade-service/src/main/java/com/bookstore/domain/vo/book/BookDetailVO.java`
- Delete: `bookstore-trade-service/src/main/java/com/bookstore/domain/vo/book/BookListVO.java`
- Delete: `bookstore-trade-service/src/main/java/com/bookstore/domain/vo/user/UserProfileVO.java`
- Delete: `bookstore-trade-service/src/main/java/com/bookstore/domain/vo/address/AddressVO.java`
- Modify: `bookstore-trade-service/src/main/java/com/bookstore/controller/OrderController.java`
- Modify: `bookstore-trade-service/src/main/java/com/bookstore/controller/InternalOrderController.java`
- Modify: `bookstore-trade-service/src/main/java/com/bookstore/service/OrderService.java`
- Modify: `bookstore-trade-service/src/main/java/com/bookstore/service/impl/OrderServiceImpl.java`

- [ ] **Step 1: POM 加入 api 依赖**

在 `bookstore-trade-service/pom.xml` 的 `<dependencies>` 中，`bookstore-common` 之后加入：

```xml
<dependency>
    <groupId>com.bookstore</groupId>
    <artifactId>bookstore-api</artifactId>
</dependency>
```

- [ ] **Step 2: application.yml 启用 OkHttp**

在 `bookstore-trade-service/src/main/resources/application.yml` 的 `jwt:` 同级位置加入：

```yaml
feign:
  okhttp:
    enabled: true
  client:
    config:
      default:
        connectTimeout: 5000
        readTimeout: 10000
  circuitbreaker:
    enabled: true
```

- [ ] **Step 3: 删除本地 client 和冗余 VO**

```bash
rm bookstore-trade-service/src/main/java/com/bookstore/client/BookServiceClient.java
rm bookstore-trade-service/src/main/java/com/bookstore/client/UserServiceClient.java
rm bookstore-trade-service/src/main/java/com/bookstore/domain/vo/book/BookDetailVO.java
rm bookstore-trade-service/src/main/java/com/bookstore/domain/vo/book/BookListVO.java
rm bookstore-trade-service/src/main/java/com/bookstore/domain/vo/user/UserProfileVO.java
rm bookstore-trade-service/src/main/java/com/bookstore/domain/vo/address/AddressVO.java
```

- [ ] **Step 4: 修改 import 和类型引用**

将以下文件中的 `import com.bookstore.domain.vo.order.OrderDetailVO;` 改为 `import com.bookstore.api.trade.dto.OrderDetailDTO;`，并把 `OrderDetailVO` 改为 `OrderDetailDTO`：
- `OrderController.java`
- `InternalOrderController.java`
- `OrderService.java`
- `OrderServiceImpl.java`

- [ ] **Step 5: Commit**

```bash
git add bookstore-trade-service/
git commit -m "refactor(trade-service): use bookstore-api, enable okhttp"
```

---

### Task 9: 清理 Admin Service 和 AI Service 中的冗余 VO

**Files:**
- Delete: `bookstore-admin-service/src/main/java/com/bookstore/domain/vo/` 下被迁移的 VO 类
- Delete: `bookstore-ai-service/src/main/java/com/bookstore/domain/vo/` 下被迁移的 VO 类

- [ ] **Step 1: 删除 admin-service 中的冗余 VO**

```bash
rm -f bookstore-admin-service/src/main/java/com/bookstore/domain/vo/book/BookDetailVO.java
rm -f bookstore-admin-service/src/main/java/com/bookstore/domain/vo/book/BookListVO.java
rm -f bookstore-admin-service/src/main/java/com/bookstore/domain/vo/user/UserProfileVO.java
rm -f bookstore-admin-service/src/main/java/com/bookstore/domain/vo/address/AddressVO.java
rm -f bookstore-admin-service/src/main/java/com/bookstore/domain/vo/order/OrderDetailVO.java
rm -f bookstore-admin-service/src/main/java/com/bookstore/domain/vo/order/OrderItemVO.java
```

- [ ] **Step 2: 删除 ai-service 中的冗余 VO**

```bash
rm -f bookstore-ai-service/src/main/java/com/bookstore/domain/vo/book/BookDetailVO.java
rm -f bookstore-ai-service/src/main/java/com/bookstore/domain/vo/book/BookListVO.java
rm -f bookstore-ai-service/src/main/java/com/bookstore/domain/vo/user/UserProfileVO.java
rm -f bookstore-ai-service/src/main/java/com/bookstore/domain/vo/address/AddressVO.java
rm -f bookstore-ai-service/src/main/java/com/bookstore/domain/vo/order/OrderDetailVO.java
rm -f bookstore-ai-service/src/main/java/com/bookstore/domain/vo/order/OrderItemVO.java
```

- [ ] **Step 3: Commit**

```bash
git add bookstore-admin-service/ bookstore-ai-service/
git commit -m "chore: remove redundant VO classes migrated to api module"
```

---

### Task 10: 编译验证

**Files:**
- All (verification only)

- [ ] **Step 1: 编译 api 模块**

Run:
```bash
./mvnw clean install -pl bookstore-api -am
```

Expected: BUILD SUCCESS

- [ ] **Step 2: 编译整个项目**

Run:
```bash
./mvnw clean compile
```

Expected: BUILD SUCCESS，无编译错误

- [ ] **Step 3: 检查常见问题**

如果编译失败，检查以下常见问题：

1. **找不到 feign-okhttp 版本**：在根 `pom.xml` 的 `<properties>` 中加入 `<feign.version>13.2.1</feign.version>`
2. **import 路径未改完**：搜索 `com.bookstore.domain.vo.book.BookDetailVO` 等旧路径，确保全部替换为 `com.bookstore.api.xxx.dto.XXXDTO`
3. **FallbackFactory 找不到 Result.fail(String) 方法**：确认 `Result.java` 中有 `fail(String)` 或 `fail(ResultCode, String)` 的重载。如果只有 `fail(ResultCode)`，需要修改为 `Result.fail(ResultCode.SERVICE_ERROR, "...")` 或添加 `fail(String)` 方法到 Result 类
4. **删除 VO 后目录为空**：检查 `domain/vo/book/`、`domain/vo/user/` 等目录是否还有其他 VO 类。如果目录为空可删除，但非必须

- [ ] **Step 4: Commit (if any fixes)**

如果有修复，commit 修复内容。

---

## Spec Coverage Check

| Spec 要求 | 对应 Task |
|---|---|
| 新建 `bookstore-api` 模块 | Task 1 |
| 迁移 Feign Client | Task 2, 3, 4 |
| 迁移共享 DTO | Task 2, 3, 4 |
| 新增 FallbackFactory | Task 2, 3, 4 |
| 根 POM 加入 api 模块 | Task 5 |
| 各业务服务依赖 api 模块 | Task 6, 7, 8 |
| 删除本地 `client/` 包 | Task 6, 7, 8 |
| OkHttp 连接池配置 | Task 6, 7, 8 |
| 包扫描处理 | 无需修改（现有 `@EnableFeignClients(basePackages = "com.bookstore")` 已覆盖） |
| 清理其他服务中的冗余 VO | Task 9 |
| 编译验证 | Task 10 |

## Placeholder Scan

- 无 TBD / TODO
- 无 "add appropriate error handling"
- 无 "similar to Task N"
- 所有代码块包含完整代码
- 所有文件路径精确

## Type Consistency Check

- `BookDetailDTO` / `BookListDTO` 在 Task 2 定义，在 Task 6, 8 中使用
- `UserProfileDTO` / `AddressDTO` 在 Task 3 定义，在 Task 7 中使用
- `OrderDetailDTO` / `OrderItemDTO` 在 Task 4 定义，在 Task 8 中使用
- 所有类型名称一致