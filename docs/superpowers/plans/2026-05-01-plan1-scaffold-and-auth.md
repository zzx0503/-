# Plan 1: 基础脚手架 + 用户认证 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 搭建 8 模块 Maven 工程脚手架，跑通注册/登录/刷新/登出 + 用户基本信息 + 收货地址 13 个接口，建立"对象分层 + Filter/Interceptor/AOP + Flyway + 单元测试"工程规范。

**Architecture:**
- 8 模块 Maven 工程：common(横切) → domain(对象) → mapper(DAO) → service(业务) → web-api/admin-api(Controller) → app(启动)，外加独立 ai 模块（本计划暂不接入业务，仅占位）
- JWT 双 Token：accessToken (2h) + refreshToken (7d)，登出加入 Redis 黑名单
- 横切三层：Filter（Cors/Jwt 解析） → Interceptor（@LoginRequired/@AdminRequired 鉴权） → AOP（@RateLimit）

**Tech Stack:** Java 17 + Spring Boot 3.2 + Maven 3.9 + MySQL 8 + Redis 7 + MyBatis-Plus 3.5 + Flyway 9 + Redisson 3.27 + jjwt 0.12 + MapStruct 1.5 + JUnit 5 + Mockito + Testcontainers

**Spec 引用：** `docs/superpowers/specs/2026-05-01-bookstore-design.md` 第 1–5 章 + 第 7.2 章前两节（认证、用户、地址）+ 第 11 章（开发环境/配置）。

**计划范围（与 spec 第 13 章 W1–W2 对齐）：**
- W1：环境准备 + 多模块 Maven 脚手架 + Flyway + 全局响应/异常 + Filter/Interceptor + Redisson 接入
- W2：JWT + Redis 黑名单 + 用户/地址模块 + 单元测试规范

**计划范围外（后续 Plan）：**
- 书籍/分类/购物车/订单/评价 → Plan 2
- AI 模块业务实现 → Plan 3
- 优惠券/秒杀 → Plan 4
- 管理后台 UI → Plan 5
- 部署 → Plan 6

**核心约定：**
- 包路径 `com.bookstore.<module>`
- 数据库名 `bookstore`，本地账号 `root / dev123`
- 默认应用端口 `8080`
- TDD 顺序：写测试 → 跑失败 → 写最小实现 → 跑通过 → commit
- 每个 Task 末尾必须 commit，不得跨 Task 累积

---

## 前置准备

### Step 0.1：本地依赖（一次性）

- [ ] 安装 JDK 17（命令 `java -version` 显示 `17.x`）
- [ ] 安装 Maven 3.9+（`mvn -v` 显示 `Apache Maven 3.9.x` 且 `Java version: 17`）
- [ ] 安装 Docker（`docker version` 正常返回）
- [ ] 安装 Git（`git --version` 正常返回）
- [ ] IDEA Ultimate / Community 安装好 Lombok + MapStruct Support 插件

### Step 0.2：项目根目录初始化

- [ ] 在 `D:\projects\bookstore` 下初始化 git 仓库

```bash
cd /d/projects/bookstore
git init
git add docs/
git commit -m "docs: initial spec & plan"
```

- [ ] 创建 `.gitignore`（拷贝以下内容到 `D:\projects\bookstore\.gitignore`）

```gitignore
# Maven
target/
*.class
.mvn/wrapper/maven-wrapper.jar

# IDE
.idea/
*.iml
*.ipr
*.iws
.vscode/
.project
.classpath
.settings/

# Logs
logs/
*.log

# Frontend (later)
node_modules/
dist/

# OS
.DS_Store
Thumbs.db

# Local secrets
.env
application-local.yml

# Docker volumes
mysql-data/
redis-data/
```

- [ ] 提交：`git add .gitignore && git commit -m "chore: add gitignore"`

---

## Phase 1: Maven 多模块脚手架（W1.1–W1.2）

### Task 1: 创建父 POM

**Files:**
- Create: `D:\projects\bookstore\pom.xml`

- [ ] **Step 1.1: 写父 POM**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.bookstore</groupId>
    <artifactId>bookstore-parent</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <packaging>pom</packaging>
    <name>bookstore-parent</name>

    <properties>
        <java.version>17</java.version>
        <maven.compiler.source>17</maven.compiler.source>
        <maven.compiler.target>17</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>

        <spring-boot.version>3.2.5</spring-boot.version>
        <mybatis-plus.version>3.5.5</mybatis-plus.version>
        <mysql.version>8.0.33</mysql.version>
        <flyway.version>9.22.3</flyway.version>
        <redisson.version>3.27.2</redisson.version>
        <jjwt.version>0.12.5</jjwt.version>
        <mapstruct.version>1.5.5.Final</mapstruct.version>
        <lombok.version>1.18.30</lombok.version>
        <lombok-mapstruct-binding.version>0.2.0</lombok-mapstruct-binding.version>
        <knife4j.version>4.4.0</knife4j.version>
        <caffeine.version>3.1.8</caffeine.version>
        <testcontainers.version>1.19.7</testcontainers.version>
    </properties>

    <modules>
        <module>bookstore-common</module>
        <module>bookstore-domain</module>
        <module>bookstore-mapper</module>
        <module>bookstore-service</module>
        <module>bookstore-ai</module>
        <module>bookstore-web-api</module>
        <module>bookstore-admin-api</module>
        <module>bookstore-app</module>
    </modules>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-dependencies</artifactId>
                <version>${spring-boot.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>

            <dependency>
                <groupId>com.bookstore</groupId>
                <artifactId>bookstore-common</artifactId>
                <version>${project.version}</version>
            </dependency>
            <dependency>
                <groupId>com.bookstore</groupId>
                <artifactId>bookstore-domain</artifactId>
                <version>${project.version}</version>
            </dependency>
            <dependency>
                <groupId>com.bookstore</groupId>
                <artifactId>bookstore-mapper</artifactId>
                <version>${project.version}</version>
            </dependency>
            <dependency>
                <groupId>com.bookstore</groupId>
                <artifactId>bookstore-service</artifactId>
                <version>${project.version}</version>
            </dependency>
            <dependency>
                <groupId>com.bookstore</groupId>
                <artifactId>bookstore-ai</artifactId>
                <version>${project.version}</version>
            </dependency>
            <dependency>
                <groupId>com.bookstore</groupId>
                <artifactId>bookstore-web-api</artifactId>
                <version>${project.version}</version>
            </dependency>
            <dependency>
                <groupId>com.bookstore</groupId>
                <artifactId>bookstore-admin-api</artifactId>
                <version>${project.version}</version>
            </dependency>

            <dependency>
                <groupId>com.baomidou</groupId>
                <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
                <version>${mybatis-plus.version}</version>
            </dependency>
            <dependency>
                <groupId>org.flywaydb</groupId>
                <artifactId>flyway-core</artifactId>
                <version>${flyway.version}</version>
            </dependency>
            <dependency>
                <groupId>org.flywaydb</groupId>
                <artifactId>flyway-mysql</artifactId>
                <version>${flyway.version}</version>
            </dependency>
            <dependency>
                <groupId>org.redisson</groupId>
                <artifactId>redisson-spring-boot-starter</artifactId>
                <version>${redisson.version}</version>
            </dependency>
            <dependency>
                <groupId>io.jsonwebtoken</groupId>
                <artifactId>jjwt-api</artifactId>
                <version>${jjwt.version}</version>
            </dependency>
            <dependency>
                <groupId>io.jsonwebtoken</groupId>
                <artifactId>jjwt-impl</artifactId>
                <version>${jjwt.version}</version>
                <scope>runtime</scope>
            </dependency>
            <dependency>
                <groupId>io.jsonwebtoken</groupId>
                <artifactId>jjwt-jackson</artifactId>
                <version>${jjwt.version}</version>
                <scope>runtime</scope>
            </dependency>
            <dependency>
                <groupId>org.mapstruct</groupId>
                <artifactId>mapstruct</artifactId>
                <version>${mapstruct.version}</version>
            </dependency>
            <dependency>
                <groupId>org.projectlombok</groupId>
                <artifactId>lombok-mapstruct-binding</artifactId>
                <version>${lombok-mapstruct-binding.version}</version>
            </dependency>
            <dependency>
                <groupId>com.github.xiaoymin</groupId>
                <artifactId>knife4j-openapi3-jakarta-spring-boot-starter</artifactId>
                <version>${knife4j.version}</version>
            </dependency>
            <dependency>
                <groupId>com.github.ben-manes.caffeine</groupId>
                <artifactId>caffeine</artifactId>
                <version>${caffeine.version}</version>
            </dependency>
            <dependency>
                <groupId>org.testcontainers</groupId>
                <artifactId>testcontainers-bom</artifactId>
                <version>${testcontainers.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <build>
        <pluginManagement>
            <plugins>
                <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-compiler-plugin</artifactId>
                    <configuration>
                        <source>17</source>
                        <target>17</target>
                        <annotationProcessorPaths>
                            <path>
                                <groupId>org.projectlombok</groupId>
                                <artifactId>lombok</artifactId>
                                <version>${lombok.version}</version>
                            </path>
                            <path>
                                <groupId>org.mapstruct</groupId>
                                <artifactId>mapstruct-processor</artifactId>
                                <version>${mapstruct.version}</version>
                            </path>
                            <path>
                                <groupId>org.projectlombok</groupId>
                                <artifactId>lombok-mapstruct-binding</artifactId>
                                <version>${lombok-mapstruct-binding.version}</version>
                            </path>
                        </annotationProcessorPaths>
                    </configuration>
                </plugin>
                <plugin>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-maven-plugin</artifactId>
                    <version>${spring-boot.version}</version>
                </plugin>
            </plugins>
        </pluginManagement>
    </build>
</project>
```

- [ ] **Step 1.2: 校验**

```bash
cd /d/projects/bookstore
mvn -N validate
```
Expected：`BUILD SUCCESS`（`-N` 表示只校验父 POM 不下钻子模块——子模块还没建，正常）

- [ ] **Step 1.3: Commit**

```bash
git add pom.xml
git commit -m "chore(pom): init parent POM with dependencyManagement"
```

---

### Task 2: 创建 8 个空子模块目录与 POM

**Files:**
- Create: `bookstore-common/pom.xml`
- Create: `bookstore-domain/pom.xml`
- Create: `bookstore-mapper/pom.xml`
- Create: `bookstore-service/pom.xml`
- Create: `bookstore-ai/pom.xml`
- Create: `bookstore-web-api/pom.xml`
- Create: `bookstore-admin-api/pom.xml`
- Create: `bookstore-app/pom.xml`

- [ ] **Step 2.1: 写 `bookstore-common/pom.xml`**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.bookstore</groupId>
        <artifactId>bookstore-parent</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>
    <artifactId>bookstore-common</artifactId>
    <packaging>jar</packaging>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-aop</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>
        <dependency>
            <groupId>org.redisson</groupId>
            <artifactId>redisson-spring-boot-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-api</artifactId>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-impl</artifactId>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-jackson</artifactId>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <scope>provided</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 2.2: 写 `bookstore-domain/pom.xml`**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.bookstore</groupId>
        <artifactId>bookstore-parent</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>
    <artifactId>bookstore-domain</artifactId>
    <packaging>jar</packaging>

    <dependencies>
        <dependency>
            <groupId>com.bookstore</groupId>
            <artifactId>bookstore-common</artifactId>
        </dependency>
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>org.mapstruct</groupId>
            <artifactId>mapstruct</artifactId>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <scope>provided</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 2.3: 写 `bookstore-mapper/pom.xml`**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.bookstore</groupId>
        <artifactId>bookstore-parent</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>
    <artifactId>bookstore-mapper</artifactId>
    <packaging>jar</packaging>

    <dependencies>
        <dependency>
            <groupId>com.bookstore</groupId>
            <artifactId>bookstore-domain</artifactId>
        </dependency>
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 2.4: 写 `bookstore-service/pom.xml`**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.bookstore</groupId>
        <artifactId>bookstore-parent</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>
    <artifactId>bookstore-service</artifactId>
    <packaging>jar</packaging>

    <dependencies>
        <dependency>
            <groupId>com.bookstore</groupId>
            <artifactId>bookstore-mapper</artifactId>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 2.5: 写 `bookstore-ai/pom.xml`** （本计划仅占位，业务在 Plan 3）

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.bookstore</groupId>
        <artifactId>bookstore-parent</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>
    <artifactId>bookstore-ai</artifactId>
    <packaging>jar</packaging>

    <dependencies>
        <dependency>
            <groupId>com.bookstore</groupId>
            <artifactId>bookstore-common</artifactId>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 2.6: 写 `bookstore-web-api/pom.xml`**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.bookstore</groupId>
        <artifactId>bookstore-parent</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>
    <artifactId>bookstore-web-api</artifactId>
    <packaging>jar</packaging>

    <dependencies>
        <dependency>
            <groupId>com.bookstore</groupId>
            <artifactId>bookstore-service</artifactId>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 2.7: 写 `bookstore-admin-api/pom.xml`**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.bookstore</groupId>
        <artifactId>bookstore-parent</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>
    <artifactId>bookstore-admin-api</artifactId>
    <packaging>jar</packaging>

    <dependencies>
        <dependency>
            <groupId>com.bookstore</groupId>
            <artifactId>bookstore-service</artifactId>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 2.8: 写 `bookstore-app/pom.xml`**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.bookstore</groupId>
        <artifactId>bookstore-parent</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>
    <artifactId>bookstore-app</artifactId>
    <packaging>jar</packaging>

    <dependencies>
        <dependency>
            <groupId>com.bookstore</groupId>
            <artifactId>bookstore-web-api</artifactId>
        </dependency>
        <dependency>
            <groupId>com.bookstore</groupId>
            <artifactId>bookstore-admin-api</artifactId>
        </dependency>
        <dependency>
            <groupId>com.bookstore</groupId>
            <artifactId>bookstore-ai</artifactId>
        </dependency>
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-core</artifactId>
        </dependency>
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-mysql</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>com.github.xiaoymin</groupId>
            <artifactId>knife4j-openapi3-jakarta-spring-boot-starter</artifactId>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <mainClass>com.bookstore.app.BookstoreApplication</mainClass>
                </configuration>
                <executions>
                    <execution>
                        <goals>
                            <goal>repackage</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 2.9: 创建每个模块的 `src/main/java` 与 `src/test/java` 空目录占位**

```bash
cd /d/projects/bookstore
for m in bookstore-common bookstore-domain bookstore-mapper bookstore-service bookstore-ai bookstore-web-api bookstore-admin-api bookstore-app; do
  mkdir -p $m/src/main/java/com/bookstore $m/src/test/java/com/bookstore
done
mkdir -p bookstore-app/src/main/resources/db/migration
mkdir -p bookstore-mapper/src/main/resources/mapper
```

- [ ] **Step 2.10: 编译验证**

```bash
mvn clean install -DskipTests
```
Expected：8 个 `BUILD SUCCESS`，最后一行 `Reactor Summary` 全 `SUCCESS`。

- [ ] **Step 2.11: Commit**

```bash
git add */pom.xml */src
git commit -m "chore(modules): scaffold 8 maven submodules"
```

---

### Task 3: 启动类与 Health Check 烟雾测试

**Files:**
- Create: `bookstore-app/src/main/java/com/bookstore/app/BookstoreApplication.java`
- Create: `bookstore-app/src/main/resources/application.yml`
- Create: `bookstore-app/src/main/resources/application-dev.yml`
- Create: `bookstore-app/src/test/java/com/bookstore/app/BookstoreApplicationTests.java`

- [ ] **Step 3.1: 在 `bookstore-app/pom.xml` 增加 `actuator` 依赖**

在 `<dependencies>` 末尾追加：

```xml
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
```

- [ ] **Step 3.2: 写启动类**

```java
package com.bookstore.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = "com.bookstore")
public class BookstoreApplication {
    public static void main(String[] args) {
        SpringApplication.run(BookstoreApplication.class, args);
    }
}
```

- [ ] **Step 3.3: 写 `application.yml`**

```yaml
spring:
  application:
    name: bookstore
  profiles:
    active: ${PROFILE:dev}
management:
  endpoints:
    web:
      exposure:
        include: health,info
```

- [ ] **Step 3.4: 写 `application-dev.yml`**（暂不接 DB/Redis，先确保启动成功）

```yaml
server:
  port: 8080
spring:
  autoconfigure:
    exclude:
      - org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
      - org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration
      - org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration
logging:
  level:
    com.bookstore: DEBUG
```
> 后续 Task 接入 DB / Redis 时把对应的 `exclude` 行删掉。

- [ ] **Step 3.5: 写 Smoke 测试**

```java
package com.bookstore.app;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class BookstoreApplicationTests {
    @Test
    void contextLoads() {
        // 仅验证 Spring 容器可加载
    }
}
```

- [ ] **Step 3.6: 跑测试（应通过）**

```bash
mvn -pl bookstore-app -am test
```
Expected：`BUILD SUCCESS`，`Tests run: 1, Failures: 0, Errors: 0`

- [ ] **Step 3.7: 启动应用并 curl health**

```bash
# 终端 A
mvn -pl bookstore-app -am spring-boot:run
# 等到 "Started BookstoreApplication"
# 终端 B
curl http://localhost:8080/actuator/health
```
Expected：`{"status":"UP"}`，停掉终端 A（Ctrl+C）。

- [ ] **Step 3.8: Commit**

```bash
git add bookstore-app
git commit -m "feat(app): bootstrap spring boot application with health check"
```

---

### Task 4: Docker 起 MySQL + Redis（一键依赖）

**Files:**
- Create: `scripts/dev-deps.docker-compose.yml`

- [ ] **Step 4.1: 写 compose**

```yaml
services:
  mysql:
    image: mysql:8.0
    container_name: bookstore-mysql
    environment:
      MYSQL_ROOT_PASSWORD: dev123
      MYSQL_DATABASE: bookstore
      TZ: Asia/Shanghai
    ports:
      - "3306:3306"
    command: ["--character-set-server=utf8mb4", "--collation-server=utf8mb4_unicode_ci"]
    volumes:
      - ./mysql-data:/var/lib/mysql
  redis:
    image: redis:7-alpine
    container_name: bookstore-redis
    ports:
      - "6379:6379"
    volumes:
      - ./redis-data:/data
```

- [ ] **Step 4.2: 启动并验证**

```bash
mkdir -p /d/projects/bookstore/scripts
# 已写入文件后
cd /d/projects/bookstore
docker compose -f scripts/dev-deps.docker-compose.yml up -d
docker exec bookstore-mysql mysql -uroot -pdev123 -e "SHOW DATABASES;" 2>/dev/null | grep bookstore
docker exec bookstore-redis redis-cli ping
```
Expected：`bookstore` 出现在数据库列表，redis 返回 `PONG`

- [ ] **Step 4.3: Commit**

```bash
git add scripts/dev-deps.docker-compose.yml
git commit -m "chore(scripts): docker compose for mysql & redis"
```

---

### Task 5: Flyway V1 初始化 schema（仅 user + address，其他表后续 Plan 补）

**Files:**
- Create: `bookstore-app/src/main/resources/db/migration/V1__init_schema.sql`

- [ ] **Step 5.1: 在 `bookstore-app/pom.xml` 增加 `mysql-connector-j`、`mybatis-plus`（运行期需要）**

在 `<dependencies>` 末尾追加：

```xml
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
        </dependency>
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
        </dependency>
```

- [ ] **Step 5.2: 写 V1 SQL**

```sql
-- V1__init_schema.sql
-- 本计划范围：仅 user + address。其他表在后续 Plan 中通过 V2/V3/... 增量添加。

CREATE TABLE `user` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT,
    `username`       VARCHAR(50)  NOT NULL,
    `password_hash`  VARCHAR(100) NOT NULL COMMENT 'BCrypt encoded',
    `nickname`       VARCHAR(50)           DEFAULT NULL,
    `avatar_key`     VARCHAR(255)          DEFAULT NULL COMMENT 'OSS object key, e.g. avatars/1.jpg',
    `email`          VARCHAR(100)          DEFAULT NULL,
    `phone`          VARCHAR(20)           DEFAULT NULL,
    `gender`         TINYINT               DEFAULT 2 COMMENT '0 女 / 1 男 / 2 未知',
    `birthday`       DATE                  DEFAULT NULL,
    `role`           VARCHAR(20)  NOT NULL DEFAULT 'USER' COMMENT 'USER / ADMIN',
    `status`         TINYINT      NOT NULL DEFAULT 1 COMMENT '0 禁用 / 1 启用',
    `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`        TINYINT(1)   NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_username` (`username`),
    UNIQUE KEY `uk_user_phone` (`phone`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户';

CREATE TABLE `address` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT,
    `user_id`         BIGINT       NOT NULL,
    `receiver`        VARCHAR(50)  NOT NULL,
    `phone`           VARCHAR(20)  NOT NULL,
    `province`        VARCHAR(50)  NOT NULL,
    `city`            VARCHAR(50)  NOT NULL,
    `district`        VARCHAR(50)  NOT NULL,
    `detail_address`  VARCHAR(255) NOT NULL,
    `is_default`      TINYINT(1)   NOT NULL DEFAULT 0,
    `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`         TINYINT(1)   NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_address_user` (`user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '收货地址';

-- 预置一个 admin 账号,密码 admin123 的 BCrypt 哈希(cost 10)。
-- 用 PasswordUtil.encode("admin123") 生成,这个哈希每次都不同(BCrypt 自带 salt),下面是其中一个有效值。
-- 验证方式:在 Task 22 完成后跑 PasswordUtil.matches("admin123", <这个哈希>) 应返回 true。
INSERT INTO `user` (`username`, `password_hash`, `nickname`, `role`, `status`)
VALUES ('admin',
        '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
        '系统管理员', 'ADMIN', 1);
```

- [ ] **Step 5.3: 更新 `application-dev.yml` 接入 DB**

把先前的 `exclude` 段删除，整段替换为：

```yaml
server:
  port: 8080
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/bookstore?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
    username: root
    password: dev123
    driver-class-name: com.mysql.cj.jdbc.Driver
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
  autoconfigure:
    exclude:
      - org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration
      - org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration
mybatis-plus:
  global-config:
    db-config:
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0
logging:
  level:
    com.bookstore: DEBUG
```

- [ ] **Step 5.4: 启动应用观察 Flyway 迁移日志**

```bash
mvn -pl bookstore-app -am spring-boot:run
```
Expected 日志中能看到：`Successfully applied 1 migration to schema "bookstore"`，停止应用。

- [ ] **Step 5.5: 验证表已建**

```bash
docker exec bookstore-mysql mysql -uroot -pdev123 bookstore -e "SHOW TABLES;"
docker exec bookstore-mysql mysql -uroot -pdev123 bookstore -e "DESC user;"
```
Expected：列出 `address`、`flyway_schema_history`、`user` 三表，`user` 表有 11 列。

- [ ] **Step 5.6: Commit**

```bash
git add bookstore-app/src/main/resources/db/migration/V1__init_schema.sql \
        bookstore-app/src/main/resources/application-dev.yml \
        bookstore-app/pom.xml
git commit -m "feat(db): flyway V1 init user & address tables"
```

---

## Phase 2: 通用响应 / 异常 / 校验框架（W1.3）

### Task 6: 统一响应体 `Result<T>`

**Files:**
- Create: `bookstore-common/src/main/java/com/bookstore/common/response/ResultCode.java`
- Create: `bookstore-common/src/main/java/com/bookstore/common/response/Result.java`
- Test: `bookstore-common/src/test/java/com/bookstore/common/response/ResultTest.java`

- [ ] **Step 6.1: 写测试**

```java
package com.bookstore.common.response;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ResultTest {

    @Test
    void success_should_return_200_and_data() {
        Result<String> r = Result.success("hello");
        assertThat(r.getCode()).isEqualTo(200);
        assertThat(r.getMsg()).isEqualTo("OK");
        assertThat(r.getData()).isEqualTo("hello");
    }

    @Test
    void success_without_data_should_return_null_data() {
        Result<Void> r = Result.success();
        assertThat(r.getCode()).isEqualTo(200);
        assertThat(r.getData()).isNull();
    }

    @Test
    void fail_should_return_given_code_and_msg() {
        Result<Void> r = Result.fail(ResultCode.PARAM_INVALID, "name is required");
        assertThat(r.getCode()).isEqualTo(400);
        assertThat(r.getMsg()).isEqualTo("name is required");
        assertThat(r.getData()).isNull();
    }

    @Test
    void fail_with_default_msg_should_use_code_default_msg() {
        Result<Void> r = Result.fail(ResultCode.UNAUTHORIZED);
        assertThat(r.getCode()).isEqualTo(401);
        assertThat(r.getMsg()).isEqualTo("未登录或登录已过期");
    }
}
```

- [ ] **Step 6.2: 跑测试（应失败：找不到类）**

```bash
mvn -pl bookstore-common -am test -Dtest=ResultTest
```
Expected：编译失败 `cannot find symbol Result`。

- [ ] **Step 6.3: 写 `ResultCode`**

```java
package com.bookstore.common.response;

import lombok.Getter;

@Getter
public enum ResultCode {

    SUCCESS(200, "OK"),
    PARAM_INVALID(400, "参数错误"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "权限不足"),
    NOT_FOUND(404, "资源不存在"),
    SERVER_ERROR(500, "服务器内部错误"),

    BIZ_ERROR(1000, "业务错误"),
    USER_NOT_FOUND(1001, "用户不存在"),
    USER_PASSWORD_WRONG(1002, "密码错误"),
    USER_BANNED(1003, "账号已被封禁"),
    USERNAME_DUPLICATED(1004, "用户名已被占用"),
    TOKEN_INVALID(1005, "Token 无效"),
    TOKEN_EXPIRED(1006, "Token 已过期"),
    REFRESH_TOKEN_INVALID(1007, "刷新令牌无效"),
    RATE_LIMIT(1008, "请求过于频繁，稍后再试");

    private final int code;
    private final String defaultMsg;

    ResultCode(int code, String defaultMsg) {
        this.code = code;
        this.defaultMsg = defaultMsg;
    }
}
```

- [ ] **Step 6.4: 写 `Result<T>`**

```java
package com.bookstore.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.ALWAYS)
public class Result<T> {

    private int code;
    private String msg;
    private T data;

    private Result() {}

    public static <T> Result<T> success(T data) {
        Result<T> r = new Result<>();
        r.code = ResultCode.SUCCESS.getCode();
        r.msg = ResultCode.SUCCESS.getDefaultMsg();
        r.data = data;
        return r;
    }

    public static <T> Result<T> success() {
        return success(null);
    }

    public static <T> Result<T> fail(ResultCode rc, String msg) {
        Result<T> r = new Result<>();
        r.code = rc.getCode();
        r.msg = msg;
        return r;
    }

    public static <T> Result<T> fail(ResultCode rc) {
        return fail(rc, rc.getDefaultMsg());
    }
}
```

- [ ] **Step 6.5: 跑测试（应通过）**

```bash
mvn -pl bookstore-common -am test -Dtest=ResultTest
```
Expected：`Tests run: 4, Failures: 0`。

- [ ] **Step 6.6: Commit**

```bash
git add bookstore-common
git commit -m "feat(common): add Result & ResultCode"
```

---

### Task 7: 分页响应体 `PageResult<T>`

**Files:**
- Create: `bookstore-common/src/main/java/com/bookstore/common/response/PageResult.java`
- Test: `bookstore-common/src/test/java/com/bookstore/common/response/PageResultTest.java`

- [ ] **Step 7.1: 写测试**

```java
package com.bookstore.common.response;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PageResultTest {

    @Test
    void of_should_compute_total_pages() {
        PageResult<String> p = PageResult.of(List.of("a", "b"), 25L, 1, 10);
        assertThat(p.getList()).containsExactly("a", "b");
        assertThat(p.getTotal()).isEqualTo(25L);
        assertThat(p.getPageNum()).isEqualTo(1);
        assertThat(p.getPageSize()).isEqualTo(10);
        assertThat(p.getPages()).isEqualTo(3);
    }

    @Test
    void empty_total_yields_zero_pages() {
        PageResult<String> p = PageResult.of(List.of(), 0L, 1, 10);
        assertThat(p.getPages()).isEqualTo(0);
    }
}
```

- [ ] **Step 7.2: 跑测试（应失败）**

```bash
mvn -pl bookstore-common test -Dtest=PageResultTest
```

- [ ] **Step 7.3: 实现**

```java
package com.bookstore.common.response;

import lombok.Data;

import java.util.List;

@Data
public class PageResult<T> {
    private List<T> list;
    private long total;
    private int pageNum;
    private int pageSize;
    private int pages;

    public static <T> PageResult<T> of(List<T> list, long total, int pageNum, int pageSize) {
        PageResult<T> p = new PageResult<>();
        p.list = list;
        p.total = total;
        p.pageNum = pageNum;
        p.pageSize = pageSize;
        p.pages = pageSize == 0 ? 0 : (int) ((total + pageSize - 1) / pageSize);
        return p;
    }
}
```

- [ ] **Step 7.4: 跑测试（应通过）**

- [ ] **Step 7.5: Commit**

```bash
git add bookstore-common
git commit -m "feat(common): add PageResult"
```

---

### Task 8: 业务异常 + 全局异常处理器

**Files:**
- Create: `bookstore-common/src/main/java/com/bookstore/common/exception/BusinessException.java`
- Create: `bookstore-common/src/main/java/com/bookstore/common/exception/AuthException.java`
- Create: `bookstore-common/src/main/java/com/bookstore/common/exception/GlobalExceptionHandler.java`
- Test: `bookstore-common/src/test/java/com/bookstore/common/exception/GlobalExceptionHandlerTest.java`

- [ ] **Step 8.1: 写 `BusinessException`**

```java
package com.bookstore.common.exception;

import com.bookstore.common.response.ResultCode;
import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    private final ResultCode resultCode;

    public BusinessException(ResultCode resultCode) {
        super(resultCode.getDefaultMsg());
        this.resultCode = resultCode;
    }

    public BusinessException(ResultCode resultCode, String msg) {
        super(msg);
        this.resultCode = resultCode;
    }
}
```

- [ ] **Step 8.2: 写 `AuthException`**

```java
package com.bookstore.common.exception;

import com.bookstore.common.response.ResultCode;

public class AuthException extends BusinessException {
    public AuthException(ResultCode resultCode) {
        super(resultCode);
    }
    public AuthException(ResultCode resultCode, String msg) {
        super(resultCode, msg);
    }
}
```

- [ ] **Step 8.3: 写测试（用 @WebMvcTest 切片）**

```java
package com.bookstore.common.exception;

import com.bookstore.common.response.ResultCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = GlobalExceptionHandlerTest.TestController.class)
@Import(GlobalExceptionHandler.class)
class GlobalExceptionHandlerTest {

    @Autowired MockMvc mvc;

    @Test
    void business_exception_returns_corresponding_code() throws Exception {
        mvc.perform(get("/test/biz"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.code").value(1001))
           .andExpect(jsonPath("$.msg").value("用户不存在"));
    }

    @Test
    void unknown_exception_returns_500() throws Exception {
        mvc.perform(get("/test/boom"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.code").value(500));
    }

    @RestController
    static class TestController {
        @GetMapping("/test/biz")
        public void biz() { throw new BusinessException(ResultCode.USER_NOT_FOUND); }

        @GetMapping("/test/boom")
        public void boom() { throw new RuntimeException("unexpected"); }
    }
}
```

- [ ] **Step 8.4: 写 `GlobalExceptionHandler`**

```java
package com.bookstore.common.exception;

import com.bookstore.common.response.Result;
import com.bookstore.common.response.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusiness(BusinessException ex, HttpServletRequest req) {
        log.warn("biz error [{}] {} -> {}", req.getMethod(), req.getRequestURI(), ex.getMessage());
        return Result.fail(ex.getResultCode(), ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValid(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
            .map(fe -> fe.getField() + " " + fe.getDefaultMessage())
            .collect(Collectors.joining("; "));
        return Result.fail(ResultCode.PARAM_INVALID, msg);
    }

    @ExceptionHandler(BindException.class)
    public Result<Void> handleBind(BindException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
            .map(FieldError::getDefaultMessage)
            .collect(Collectors.joining("; "));
        return Result.fail(ResultCode.PARAM_INVALID, msg);
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleAny(Exception ex, HttpServletRequest req) {
        log.error("unexpected error [{}] {}", req.getMethod(), req.getRequestURI(), ex);
        return Result.fail(ResultCode.SERVER_ERROR, "服务器繁忙，请稍后再试");
    }
}
```

- [ ] **Step 8.5: 跑测试（应通过）**

```bash
mvn -pl bookstore-common test -Dtest=GlobalExceptionHandlerTest
```

- [ ] **Step 8.6: Commit**

```bash
git add bookstore-common
git commit -m "feat(common): add BusinessException + GlobalExceptionHandler"
```

---

## Phase 3: BaseEntity + MyBatis-Plus 自动填充（W1.4）

### Task 9: `BaseEntity`

**Files:**
- Create: `bookstore-domain/src/main/java/com/bookstore/domain/base/BaseEntity.java`

- [ ] **Step 9.1: 实现**

```java
package com.bookstore.domain.base;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public abstract class BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer deleted;
}
```

- [ ] **Step 9.2: Commit**

```bash
git add bookstore-domain
git commit -m "feat(domain): BaseEntity with id/timestamps/logic-delete"
```

---

### Task 10: MyBatis-Plus 配置 + 自动填充处理器

**Files:**
- Create: `bookstore-app/src/main/java/com/bookstore/app/config/MybatisPlusConfig.java`
- Create: `bookstore-app/src/main/java/com/bookstore/app/config/MetaObjectHandlerImpl.java`

- [ ] **Step 10.1: 实现 MetaObjectHandler**

```java
package com.bookstore.app.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class MetaObjectHandlerImpl implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, now);
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, now);
        this.strictInsertFill(metaObject, "deleted", Integer.class, 0);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
    }
}
```

- [ ] **Step 10.2: 实现 MybatisPlusConfig**

```java
package com.bookstore.app.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.bookstore.mapper")
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}
```

- [ ] **Step 10.3: Commit**

```bash
git add bookstore-app
git commit -m "feat(app): mybatis-plus config + auto-fill handler"
```

---

## Phase 4: 用户上下文 + Filter + Interceptor 框架（W1.5）

### Task 11: `UserContext` ThreadLocal

**Files:**
- Create: `bookstore-common/src/main/java/com/bookstore/common/context/UserContext.java`
- Create: `bookstore-common/src/main/java/com/bookstore/common/context/CurrentUser.java`
- Test: `bookstore-common/src/test/java/com/bookstore/common/context/UserContextTest.java`

- [ ] **Step 11.1: 写测试**

```java
package com.bookstore.common.context;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserContextTest {

    @AfterEach
    void clean() { UserContext.clear(); }

    @Test
    void set_then_get_returns_same_user() {
        CurrentUser u = new CurrentUser(1L, "alice", "USER");
        UserContext.set(u);
        assertThat(UserContext.get().getUserId()).isEqualTo(1L);
        assertThat(UserContext.get().getUsername()).isEqualTo("alice");
    }

    @Test
    void clear_drops_user() {
        UserContext.set(new CurrentUser(1L, "alice", "USER"));
        UserContext.clear();
        assertThat(UserContext.get()).isNull();
    }

    @Test
    void requireUserId_throws_when_no_user() {
        org.assertj.core.api.Assertions.assertThatThrownBy(UserContext::requireUserId)
            .isInstanceOf(IllegalStateException.class);
    }
}
```

- [ ] **Step 11.2: 写 `CurrentUser`**

```java
package com.bookstore.common.context;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CurrentUser {
    private Long userId;
    private String username;
    private String role;
}
```

- [ ] **Step 11.3: 写 `UserContext`**

```java
package com.bookstore.common.context;

public final class UserContext {

    private static final ThreadLocal<CurrentUser> HOLDER = new ThreadLocal<>();

    private UserContext() {}

    public static void set(CurrentUser user) { HOLDER.set(user); }

    public static CurrentUser get() { return HOLDER.get(); }

    public static void clear() { HOLDER.remove(); }

    public static Long requireUserId() {
        CurrentUser u = HOLDER.get();
        if (u == null) {
            throw new IllegalStateException("UserContext is empty");
        }
        return u.getUserId();
    }
}
```

- [ ] **Step 11.4: 跑测试，应通过**

```bash
mvn -pl bookstore-common test -Dtest=UserContextTest
```

- [ ] **Step 11.5: Commit**

```bash
git add bookstore-common
git commit -m "feat(common): UserContext + CurrentUser"
```

---

### Task 12: `CorsFilter`（开发期允许全部跨域）

**Files:**
- Create: `bookstore-common/src/main/java/com/bookstore/common/filter/CorsFilter.java`

- [ ] **Step 12.1: 实现**

```java
package com.bookstore.common.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorsFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest req,
                                    HttpServletResponse res,
                                    FilterChain chain) throws ServletException, IOException {
        String origin = req.getHeader("Origin");
        if (origin != null) {
            res.setHeader("Access-Control-Allow-Origin", origin);
            res.setHeader("Access-Control-Allow-Credentials", "true");
        }
        res.setHeader("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS");
        res.setHeader("Access-Control-Allow-Headers", "Authorization,Content-Type,Accept,X-Requested-With");
        res.setHeader("Access-Control-Max-Age", "3600");
        if ("OPTIONS".equalsIgnoreCase(req.getMethod())) {
            res.setStatus(HttpServletResponse.SC_NO_CONTENT);
            return;
        }
        chain.doFilter(req, res);
    }
}
```

- [ ] **Step 12.2: Commit**

```bash
git add bookstore-common
git commit -m "feat(common): CorsFilter"
```

---

### Task 13: `@LoginRequired` + `@AdminRequired` 注解

**Files:**
- Create: `bookstore-common/src/main/java/com/bookstore/common/annotation/LoginRequired.java`
- Create: `bookstore-common/src/main/java/com/bookstore/common/annotation/AdminRequired.java`

- [ ] **Step 13.1: 写两个注解**

```java
package com.bookstore.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface LoginRequired {
}
```

```java
package com.bookstore.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface AdminRequired {
}
```

- [ ] **Step 13.2: Commit**

```bash
git add bookstore-common
git commit -m "feat(common): @LoginRequired + @AdminRequired annotations"
```

---

### Task 14: `LoginInterceptor` + `AdminInterceptor`

**Files:**
- Create: `bookstore-common/src/main/java/com/bookstore/common/interceptor/LoginInterceptor.java`
- Create: `bookstore-common/src/main/java/com/bookstore/common/interceptor/AdminInterceptor.java`

- [ ] **Step 14.1: 写 `LoginInterceptor`**

```java
package com.bookstore.common.interceptor;

import com.bookstore.common.annotation.LoginRequired;
import com.bookstore.common.context.UserContext;
import com.bookstore.common.exception.AuthException;
import com.bookstore.common.response.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.lang.reflect.Method;

@Component
public class LoginInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) {
        if (!(handler instanceof HandlerMethod hm)) return true;
        Method method = hm.getMethod();
        boolean methodAnnotated = method.isAnnotationPresent(LoginRequired.class);
        boolean classAnnotated = method.getDeclaringClass().isAnnotationPresent(LoginRequired.class);
        if (!methodAnnotated && !classAnnotated) return true;
        if (UserContext.get() == null) {
            throw new AuthException(ResultCode.UNAUTHORIZED);
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest req, HttpServletResponse res, Object handler, Exception ex) {
        UserContext.clear();
    }
}
```

- [ ] **Step 14.2: 写 `AdminInterceptor`**

```java
package com.bookstore.common.interceptor;

import com.bookstore.common.annotation.AdminRequired;
import com.bookstore.common.context.CurrentUser;
import com.bookstore.common.context.UserContext;
import com.bookstore.common.exception.AuthException;
import com.bookstore.common.response.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AdminInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) {
        if (!(handler instanceof HandlerMethod hm)) return true;
        boolean needsAdmin = req.getRequestURI().startsWith("/admin-api/")
            || hm.hasMethodAnnotation(AdminRequired.class)
            || hm.getBeanType().isAnnotationPresent(AdminRequired.class);
        if (!needsAdmin) return true;
        CurrentUser u = UserContext.get();
        if (u == null) throw new AuthException(ResultCode.UNAUTHORIZED);
        if (!"ADMIN".equals(u.getRole())) throw new AuthException(ResultCode.FORBIDDEN);
        return true;
    }
}
```

- [ ] **Step 14.3: Commit**

```bash
git add bookstore-common
git commit -m "feat(common): Login/Admin interceptors"
```

---

### Task 15: `WebMvcConfig` 注册拦截器

**Files:**
- Create: `bookstore-app/src/main/java/com/bookstore/app/config/WebMvcConfig.java`

- [ ] **Step 15.1: 实现**

```java
package com.bookstore.app.config;

import com.bookstore.common.interceptor.AdminInterceptor;
import com.bookstore.common.interceptor.LoginInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final LoginInterceptor loginInterceptor;
    private final AdminInterceptor adminInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginInterceptor)
            .addPathPatterns("/api/**", "/admin-api/**")
            .excludePathPatterns(
                "/api/auth/login",
                "/api/auth/register",
                "/api/auth/refresh",
                "/error", "/doc.html", "/v3/api-docs/**", "/webjars/**", "/actuator/**"
            );
        registry.addInterceptor(adminInterceptor)
            .addPathPatterns("/admin-api/**")
            .excludePathPatterns("/admin-api/auth/login");
    }
}
```

- [ ] **Step 15.2: Commit**

```bash
git add bookstore-app
git commit -m "feat(app): register interceptors via WebMvcConfig"
```

---

## Phase 5: Redis / Redisson 接入 + `@RateLimit`（W2.1）

### Task 16: Redis 配置 + RedissonConfig

**Files:**
- Modify: `bookstore-app/src/main/resources/application-dev.yml`
- Create: `bookstore-app/src/main/java/com/bookstore/app/config/RedisConfig.java`
- Create: `bookstore-common/src/main/java/com/bookstore/common/redisson/RedissonConfig.java`

- [ ] **Step 16.1: 删除 dev.yml 里 Redis 的 `exclude`，加 redis 配置**

把 `application-dev.yml` 里的 `exclude` 段全删掉，并在 `spring:` 下加：

```yaml
  data:
    redis:
      host: localhost
      port: 6379
      timeout: 3s
      lettuce:
        pool:
          max-active: 16
          max-idle: 8
```

- [ ] **Step 16.2: 写 `RedisConfig`**

```java
package com.bookstore.app.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory cf) {
        ObjectMapper om = new ObjectMapper();
        om.registerModule(new JavaTimeModule());
        om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
            .allowIfSubType(Object.class).build();
        om.activateDefaultTyping(ptv, DefaultTyping.NON_FINAL);

        Jackson2JsonRedisSerializer<Object> json = new Jackson2JsonRedisSerializer<>(om, Object.class);
        StringRedisSerializer str = new StringRedisSerializer();

        RedisTemplate<String, Object> t = new RedisTemplate<>();
        t.setConnectionFactory(cf);
        t.setKeySerializer(str);
        t.setHashKeySerializer(str);
        t.setValueSerializer(json);
        t.setHashValueSerializer(json);
        t.afterPropertiesSet();
        return t;
    }

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory cf) {
        return new StringRedisTemplate(cf);
    }
}
```

- [ ] **Step 16.3: 写 `RedissonConfig`**

> 注：使用 Spring Boot Starter 时，Redisson 会自动从 `spring.data.redis.*` 拼出配置；但显式声明便于后续 Plan 4 加 Stream/DelayedQueue 时调整。

```java
package com.bookstore.common.redisson;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedissonConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String host;

    @Value("${spring.data.redis.port:6379}")
    private int port;

    @Value("${spring.data.redis.password:}")
    private String password;

    @Bean(destroyMethod = "shutdown")
    @ConditionalOnMissingBean(RedissonClient.class)
    public RedissonClient redissonClient() {
        Config config = new Config();
        var single = config.useSingleServer()
            .setAddress("redis://" + host + ":" + port)
            .setConnectionPoolSize(16)
            .setConnectionMinimumIdleSize(4);
        if (password != null && !password.isEmpty()) {
            single.setPassword(password);
        }
        return Redisson.create(config);
    }
}
```

- [ ] **Step 16.4: 启动应用并 ping**

```bash
mvn -pl bookstore-app -am spring-boot:run
# 启动成功后停掉
```
观察日志含 `Redisson 3.27.x` 启动信息。

- [ ] **Step 16.5: Commit**

```bash
git add bookstore-app bookstore-common
git commit -m "feat(infra): redis + redisson config"
```

---

### Task 17: `@RateLimit` + `RateLimitAspect`

**Files:**
- Create: `bookstore-common/src/main/java/com/bookstore/common/annotation/RateLimit.java`
- Create: `bookstore-common/src/main/java/com/bookstore/common/aspect/RateLimitAspect.java`
- Test: `bookstore-common/src/test/java/com/bookstore/common/aspect/RateLimitAspectTest.java`（暂用 Mockito 验证调用 Redisson API；端到端在 Phase 6 集成）

- [ ] **Step 17.1: 写注解**

```java
package com.bookstore.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    /** 唯一 key，支持 SpEL 占位（本期先用静态字符串 + 客户端 IP） */
    String key();
    /** 速率（每秒许可数） */
    int qps() default 5;
    /** 桶容量 */
    int burst() default 10;
}
```

- [ ] **Step 17.2: 写 Aspect**

```java
package com.bookstore.common.aspect;

import com.bookstore.common.annotation.RateLimit;
import com.bookstore.common.exception.BusinessException;
import com.bookstore.common.response.ResultCode;
import com.bookstore.common.util.IpUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitAspect {

    private final RedissonClient redisson;

    @Around("@annotation(rl)")
    public Object around(ProceedingJoinPoint pjp, RateLimit rl) throws Throwable {
        String key = "rate:" + rl.key() + ":" + currentIp();
        RRateLimiter limiter = redisson.getRateLimiter(key);
        limiter.trySetRate(RateType.OVERALL, rl.burst(), 1, RateIntervalUnit.SECONDS);
        if (!limiter.tryAcquire(1)) {
            log.warn("rate limit hit key={}", key);
            throw new BusinessException(ResultCode.RATE_LIMIT);
        }
        return pjp.proceed();
    }

    private String currentIp() {
        ServletRequestAttributes attrs =
            (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) return "unknown";
        HttpServletRequest req = attrs.getRequest();
        return IpUtil.getClientIp(req);
    }
}
```

- [ ] **Step 17.3: 写 `IpUtil`**

```java
package com.bookstore.common.util;

import jakarta.servlet.http.HttpServletRequest;

public final class IpUtil {

    private IpUtil() {}

    public static String getClientIp(HttpServletRequest req) {
        String ip = req.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isBlank()) {
            int comma = ip.indexOf(',');
            return comma > -1 ? ip.substring(0, comma).trim() : ip.trim();
        }
        ip = req.getHeader("X-Real-IP");
        if (ip != null && !ip.isBlank()) return ip.trim();
        return req.getRemoteAddr();
    }
}
```

- [ ] **Step 17.4: 写 Aspect 单测**

```java
package com.bookstore.common.aspect;

import com.bookstore.common.annotation.RateLimit;
import com.bookstore.common.exception.BusinessException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RedissonClient;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RateLimitAspectTest {

    private RedissonClient redisson;
    private RRateLimiter limiter;
    private RateLimitAspect aspect;

    @BeforeEach
    void setup() {
        redisson = mock(RedissonClient.class);
        limiter = mock(RRateLimiter.class);
        when(redisson.getRateLimiter(Mockito.anyString())).thenReturn(limiter);
        aspect = new RateLimitAspect(redisson);

        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRemoteAddr("127.0.0.1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(req));
    }

    @Test
    void allows_when_token_available() throws Throwable {
        when(limiter.tryAcquire(1)).thenReturn(true);
        ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
        when(pjp.proceed()).thenReturn("ok");

        Object out = aspect.around(pjp, ann("login", 5, 10));
        assertThat(out).isEqualTo("ok");
    }

    @Test
    void rejects_when_no_token() throws Throwable {
        when(limiter.tryAcquire(1)).thenReturn(false);
        ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);

        assertThatThrownBy(() -> aspect.around(pjp, ann("login", 5, 10)))
            .isInstanceOf(BusinessException.class);
    }

    private RateLimit ann(String key, int qps, int burst) {
        return new RateLimit() {
            @Override public Class<? extends java.lang.annotation.Annotation> annotationType() { return RateLimit.class; }
            @Override public String key() { return key; }
            @Override public int qps() { return qps; }
            @Override public int burst() { return burst; }
        };
    }
}
```

- [ ] **Step 17.5: 跑测试**

```bash
mvn -pl bookstore-common test -Dtest=RateLimitAspectTest
```
Expected：2 通过。

- [ ] **Step 17.6: Commit**

```bash
git add bookstore-common
git commit -m "feat(common): @RateLimit + RateLimitAspect with redisson RRateLimiter"
```

---

## Phase 6: JWT + 认证过滤器（W2.2–W2.4）

### Task 18: `JwtUtil`

**Files:**
- Create: `bookstore-common/src/main/java/com/bookstore/common/util/JwtUtil.java`
- Test: `bookstore-common/src/test/java/com/bookstore/common/util/JwtUtilTest.java`

- [ ] **Step 18.1: 写测试**

```java
package com.bookstore.common.util;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtUtilTest {

    private JwtUtil jwt;

    @BeforeEach
    void setup() {
        // 64-byte secret for HS256/HS512 safety
        SecretKey key = Keys.hmacShaKeyFor("0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF".getBytes());
        jwt = new JwtUtil(key, Duration.ofHours(2), Duration.ofDays(7));
    }

    @Test
    void issue_then_parse_recovers_claims() {
        String token = jwt.issueAccessToken(42L, "alice", "USER");
        JwtUtil.UserClaims c = jwt.parseAccess(token);
        assertThat(c.userId()).isEqualTo(42L);
        assertThat(c.username()).isEqualTo("alice");
        assertThat(c.role()).isEqualTo("USER");
    }

    @Test
    void access_and_refresh_have_different_type_claim() {
        String access = jwt.issueAccessToken(1L, "u", "USER");
        String refresh = jwt.issueRefreshToken(1L, "u", "USER");
        assertThat(jwt.parseAccess(access)).isNotNull();
        assertThatThrownBy(() -> jwt.parseAccess(refresh))
            .isInstanceOf(JwtException.class);
    }

    @Test
    void tampered_signature_is_rejected() {
        String token = jwt.issueAccessToken(1L, "u", "USER");
        String tampered = token.substring(0, token.length() - 2) + "ab";
        assertThatThrownBy(() -> jwt.parseAccess(tampered))
            .isInstanceOf(JwtException.class);
    }
}
```

- [ ] **Step 18.2: 实现 `JwtUtil`**

```java
package com.bookstore.common.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.util.Date;
import java.util.UUID;

public class JwtUtil {

    public static final String TYPE_ACCESS  = "access";
    public static final String TYPE_REFRESH = "refresh";

    private final SecretKey key;
    private final Duration accessTtl;
    private final Duration refreshTtl;

    public JwtUtil(SecretKey key, Duration accessTtl, Duration refreshTtl) {
        this.key = key;
        this.accessTtl = accessTtl;
        this.refreshTtl = refreshTtl;
    }

    public String issueAccessToken(Long userId, String username, String role) {
        return issue(userId, username, role, TYPE_ACCESS, accessTtl);
    }

    public String issueRefreshToken(Long userId, String username, String role) {
        return issue(userId, username, role, TYPE_REFRESH, refreshTtl);
    }

    private String issue(Long userId, String username, String role, String type, Duration ttl) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
            .id(UUID.randomUUID().toString())
            .subject(String.valueOf(userId))
            .claim("username", username)
            .claim("role", role)
            .claim("type", type)
            .issuedAt(new Date(now))
            .expiration(new Date(now + ttl.toMillis()))
            .signWith(key)
            .compact();
    }

    public UserClaims parseAccess(String token) {
        return parse(token, TYPE_ACCESS);
    }

    public UserClaims parseRefresh(String token) {
        return parse(token, TYPE_REFRESH);
    }

    private UserClaims parse(String token, String expectedType) {
        Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        String type = claims.get("type", String.class);
        if (!expectedType.equals(type)) {
            throw new JwtException("token type mismatch: expected " + expectedType + ", got " + type);
        }
        return new UserClaims(
            Long.parseLong(claims.getSubject()),
            claims.get("username", String.class),
            claims.get("role", String.class),
            claims.getId(),
            claims.getExpiration().toInstant().toEpochMilli()
        );
    }

    public record UserClaims(Long userId, String username, String role, String jti, long expiresAtEpochMs) {}
}
```

- [ ] **Step 18.3: 跑测试，应通过**

```bash
mvn -pl bookstore-common test -Dtest=JwtUtilTest
```

- [ ] **Step 18.4: Commit**

```bash
git add bookstore-common
git commit -m "feat(common): JwtUtil for access/refresh tokens"
```

---

### Task 19: 在 Spring 中配置 `JwtUtil` Bean + 配置项

**Files:**
- Modify: `bookstore-app/src/main/resources/application.yml`
- Create: `bookstore-app/src/main/java/com/bookstore/app/config/JwtConfig.java`

- [ ] **Step 19.1: 在 `application.yml` 增加 jwt 段**

```yaml
jwt:
  secret: ${JWT_SECRET:0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF}
  access-token-ttl: 2h
  refresh-token-ttl: 7d
```

- [ ] **Step 19.2: 写 `JwtConfig`**

```java
package com.bookstore.app.config;

import com.bookstore.common.util.JwtUtil;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Configuration
public class JwtConfig {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-token-ttl}")
    private Duration accessTtl;

    @Value("${jwt.refresh-token-ttl}")
    private Duration refreshTtl;

    @Bean
    public JwtUtil jwtUtil() {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        return new JwtUtil(key, accessTtl, refreshTtl);
    }
}
```

- [ ] **Step 19.3: Commit**

```bash
git add bookstore-app
git commit -m "feat(app): wire JwtUtil bean from properties"
```

---

### Task 20: Token 黑名单服务 `TokenBlacklistService`

**Files:**
- Create: `bookstore-service/src/main/java/com/bookstore/service/auth/TokenBlacklistService.java`
- Test: `bookstore-service/src/test/java/com/bookstore/service/auth/TokenBlacklistServiceTest.java`

- [ ] **Step 20.1: 在 `bookstore-service/pom.xml` 末尾追加 `bookstore-common` + 测试依赖**

```xml
        <dependency>
            <groupId>com.bookstore</groupId>
            <artifactId>bookstore-common</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
```
（mapper 已传递依赖 common，但 service 直接用 common 类，显式声明便于阅读。）

- [ ] **Step 20.2: 写测试**

```java
package com.bookstore.service.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TokenBlacklistServiceTest {

    private StringRedisTemplate redis;
    private ValueOperations<String, String> ops;
    private TokenBlacklistService svc;

    @BeforeEach
    void setup() {
        redis = mock(StringRedisTemplate.class);
        ops = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(ops);
        svc = new TokenBlacklistService(redis);
    }

    @Test
    void revoke_writes_jti_with_remaining_ttl() {
        long expiresAt = System.currentTimeMillis() + 60_000;
        svc.revoke("jti-1", expiresAt);
        verify(ops).set(eq("blacklist:jti-1"), eq("1"), any());
    }

    @Test
    void isRevoked_returns_true_when_present() {
        when(redis.hasKey("blacklist:jti-2")).thenReturn(true);
        assertThat(svc.isRevoked("jti-2")).isTrue();
    }

    private static String eq(String s) { return org.mockito.ArgumentMatchers.eq(s); }
}
```

- [ ] **Step 20.3: 实现**

```java
package com.bookstore.service.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private static final String PREFIX = "blacklist:";

    private final StringRedisTemplate redis;

    /** 把 jti 加入黑名单，TTL = token 剩余有效期（过期后 key 自然消失，节省内存） */
    public void revoke(String jti, long expiresAtEpochMs) {
        long remaining = expiresAtEpochMs - Instant.now().toEpochMilli();
        if (remaining <= 0) return;
        redis.opsForValue().set(PREFIX + jti, "1", Duration.ofMillis(remaining));
    }

    public boolean isRevoked(String jti) {
        Boolean has = redis.hasKey(PREFIX + jti);
        return Boolean.TRUE.equals(has);
    }
}
```

- [ ] **Step 20.4: 跑测试**

```bash
mvn -pl bookstore-service -am test -Dtest=TokenBlacklistServiceTest
```

- [ ] **Step 20.5: Commit**

```bash
git add bookstore-service
git commit -m "feat(auth): TokenBlacklistService backed by Redis"
```

---

### Task 21: `JwtAuthenticationFilter` — 解析 Token + 写入 UserContext

**Files:**
- Create: `bookstore-common/src/main/java/com/bookstore/common/filter/JwtAuthenticationFilter.java`

- [ ] **Step 21.1: 实现**

```java
package com.bookstore.common.filter;

import com.bookstore.common.context.CurrentUser;
import com.bookstore.common.context.UserContext;
import com.bookstore.common.util.JwtUtil;
import com.bookstore.common.util.JwtUtil.UserClaims;
import com.bookstore.service.auth.TokenBlacklistService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String HEADER = "Authorization";
    private static final String PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;
    private final TokenBlacklistService blacklist;

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String h = req.getHeader(HEADER);
        if (h != null && h.startsWith(PREFIX)) {
            String token = h.substring(PREFIX.length()).trim();
            try {
                UserClaims c = jwtUtil.parseAccess(token);
                if (blacklist.isRevoked(c.jti())) {
                    log.debug("revoked token jti={}", c.jti());
                } else {
                    UserContext.set(new CurrentUser(c.userId(), c.username(), c.role()));
                }
            } catch (JwtException ex) {
                log.debug("invalid jwt: {}", ex.getMessage());
                // 不在 Filter 抛异常，让 LoginInterceptor 根据 @LoginRequired 决定是否拒绝
            }
        }
        try {
            chain.doFilter(req, res);
        } finally {
            UserContext.clear();
        }
    }
}
```

- [ ] **Step 21.2: 把 `bookstore-common/pom.xml` 增加 service 依赖（filter 需要 `TokenBlacklistService`）**

> 反向依赖会造成循环。**正确做法**是把 `TokenBlacklistService` 留在 service 模块，但 filter 在 common 模块。解决：把 `JwtAuthenticationFilter` 移到 service 模块。

**修订：删除 `bookstore-common/.../filter/JwtAuthenticationFilter.java`，重新建到 service 模块。**

- [ ] **Step 21.3: 把 filter 移到 service 模块**

```bash
mkdir -p /d/projects/bookstore/bookstore-service/src/main/java/com/bookstore/service/auth/filter
rm -f /d/projects/bookstore/bookstore-common/src/main/java/com/bookstore/common/filter/JwtAuthenticationFilter.java
```

写到 `bookstore-service/src/main/java/com/bookstore/service/auth/filter/JwtAuthenticationFilter.java`，类内 package 改为：

```java
package com.bookstore.service.auth.filter;
```

其余代码同 Step 21.1。

- [ ] **Step 21.4: 编译验证**

```bash
mvn -pl bookstore-service -am compile
```
Expected：成功。

- [ ] **Step 21.5: Commit**

```bash
git add bookstore-common bookstore-service
git commit -m "feat(auth): JwtAuthenticationFilter (in service module to avoid circular dep)"
```

---

### Task 22: `PasswordUtil`（BCrypt）

**Files:**
- Create: `bookstore-common/src/main/java/com/bookstore/common/util/PasswordUtil.java`
- Test: `bookstore-common/src/test/java/com/bookstore/common/util/PasswordUtilTest.java`

- [ ] **Step 22.1: 写测试**

```java
package com.bookstore.common.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordUtilTest {
    @Test
    void encode_then_matches() {
        String hash = PasswordUtil.encode("hello123");
        assertThat(PasswordUtil.matches("hello123", hash)).isTrue();
        assertThat(PasswordUtil.matches("HELLO123", hash)).isFalse();
    }

    @Test
    void each_encode_yields_different_hash() {
        String h1 = PasswordUtil.encode("same");
        String h2 = PasswordUtil.encode("same");
        assertThat(h1).isNotEqualTo(h2);  // BCrypt salts each call
    }
}
```

- [ ] **Step 22.2: 在 `bookstore-common/pom.xml` 末尾追加 spring-security-crypto**

```xml
        <dependency>
            <groupId>org.springframework.security</groupId>
            <artifactId>spring-security-crypto</artifactId>
        </dependency>
```

- [ ] **Step 22.3: 实现**

```java
package com.bookstore.common.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public final class PasswordUtil {

    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder(10);

    private PasswordUtil() {}

    public static String encode(String raw) { return ENCODER.encode(raw); }

    public static boolean matches(String raw, String hash) {
        if (raw == null || hash == null) return false;
        return ENCODER.matches(raw, hash);
    }
}
```

- [ ] **Step 22.4: 跑测试**

```bash
mvn -pl bookstore-common test -Dtest=PasswordUtilTest
```

- [ ] **Step 22.5: Commit**

```bash
git add bookstore-common
git commit -m "feat(common): PasswordUtil with BCrypt"
```

---

## Phase 7: 用户认证业务 (注册/登录/刷新/登出)

**目标:** 用 TDD 实现 4 个核心认证接口。先写 service 层(可单测),再封 controller(切片测)。`AuthService` 不依赖 `HttpServletRequest`,只接收纯参数,方便测试。

**端点最终形态:**
- `POST /api/auth/register` — 用户名+密码+手机号注册
- `POST /api/auth/login` — 用户名/手机号+密码 → access+refresh
- `POST /api/auth/refresh` — refresh token → 新 access token
- `POST /api/auth/logout` — 把当前 access token 的 jti 加入黑名单

---

### Task 23: User 实体 + UserMapper

**Files:**
- Create: `bookstore-domain/src/main/java/com/bookstore/domain/po/User.java`
- Create: `bookstore-mapper/src/main/java/com/bookstore/mapper/UserMapper.java`

User 表已经在 `V1__init_schema.sql` 里建好(参见 Task 5),这里只是建对应的实体和 Mapper。

- [ ] **Step 23.1: 创建 User 实体**

```java
package com.bookstore.domain.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.bookstore.domain.po.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user")
public class User extends BaseEntity {

    private String username;
    private String phone;
    private String email;

    /** BCrypt 哈希,绝不返回前端 */
    @TableField("password_hash")
    private String passwordHash;

    private String nickname;

    /** OSS 对象 key,渲染时拼前缀 */
    @TableField("avatar_key")
    private String avatarKey;

    /** 0 女 / 1 男 / 2 未知 */
    private Integer gender;

    private LocalDate birthday;

    /** USER / ADMIN */
    private String role;

    /** 0 禁用 / 1 启用 */
    private Integer status;
}
```

- [ ] **Step 23.2: UserMapper(只继承 BaseMapper,后续再加自定义 SQL)**

```java
package com.bookstore.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bookstore.domain.po.User;

public interface UserMapper extends BaseMapper<User> {
}
```

- [ ] **Step 23.3: 编译验证**

```bash
mvn -pl bookstore-mapper -am compile
```

- [ ] **Step 23.4: Commit**

```bash
git add bookstore-domain bookstore-mapper
git commit -m "feat(domain): User entity and UserMapper"
```

---

### Task 24: 认证相关 DTO/VO

**Files:**
- Create: `bookstore-domain/src/main/java/com/bookstore/domain/dto/auth/RegisterDTO.java`
- Create: `bookstore-domain/src/main/java/com/bookstore/domain/dto/auth/LoginDTO.java`
- Create: `bookstore-domain/src/main/java/com/bookstore/domain/dto/auth/RefreshTokenDTO.java`
- Create: `bookstore-domain/src/main/java/com/bookstore/domain/vo/auth/TokenVO.java`
- Create: `bookstore-domain/src/main/java/com/bookstore/domain/vo/auth/UserBriefVO.java`

- [ ] **Step 24.1: RegisterDTO(带 Bean Validation)**

```java
package com.bookstore.domain.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterDTO {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 20, message = "用户名长度 3-20")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "用户名只允许字母数字下划线")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 32, message = "密码长度 6-32")
    private String password;

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;
}
```

- [ ] **Step 24.2: LoginDTO**

```java
package com.bookstore.domain.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginDTO {

    /** 用户名或手机号 */
    @NotBlank(message = "账号不能为空")
    private String account;

    @NotBlank(message = "密码不能为空")
    private String password;
}
```

- [ ] **Step 24.3: RefreshTokenDTO**

```java
package com.bookstore.domain.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RefreshTokenDTO {

    @NotBlank(message = "refreshToken 不能为空")
    private String refreshToken;
}
```

- [ ] **Step 24.4: TokenVO**

```java
package com.bookstore.domain.vo.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TokenVO {

    private String accessToken;
    private String refreshToken;

    /** access token 过期秒数,前端用来提前刷新 */
    private Long expiresIn;

    private UserBriefVO user;
}
```

- [ ] **Step 24.5: UserBriefVO**

```java
package com.bookstore.domain.vo.auth;

import lombok.Data;

@Data
public class UserBriefVO {

    private Long id;
    private String username;
    private String nickname;
    private String avatarUrl;
    private String role;
}
```

- [ ] **Step 24.6: Commit**

```bash
git add bookstore-domain
git commit -m "feat(domain): auth DTOs and VOs"
```

---

### Task 25: AuthService.register — TDD

**Files:**
- Create: `bookstore-service/src/main/java/com/bookstore/service/auth/AuthService.java`
- Create: `bookstore-service/src/main/java/com/bookstore/service/auth/impl/AuthServiceImpl.java`
- Create: `bookstore-service/src/test/java/com/bookstore/service/auth/AuthServiceRegisterTest.java`

注册需要:
- 用户名唯一(查 user 表)
- 手机号唯一
- 密码 bcrypt 后入库
- role 默认 USER, status 默认 1
- 注册成功后**直接返回 TokenVO**(自动登录)

- [ ] **Step 25.1: 接口定义**

```java
package com.bookstore.service.auth;

import com.bookstore.domain.dto.auth.LoginDTO;
import com.bookstore.domain.dto.auth.RefreshTokenDTO;
import com.bookstore.domain.dto.auth.RegisterDTO;
import com.bookstore.domain.vo.auth.TokenVO;

public interface AuthService {

    TokenVO register(RegisterDTO dto);

    TokenVO login(LoginDTO dto);

    TokenVO refresh(RefreshTokenDTO dto);

    /** 把当前 access token 的 jti 加入黑名单 */
    void logout(String accessToken);
}
```

- [ ] **Step 25.2: 写 AuthServiceImpl 骨架(只实现 register,其它先抛异常)**

```java
package com.bookstore.service.auth.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bookstore.common.exception.BusinessException;
import com.bookstore.common.result.ResultCode;
import com.bookstore.common.util.PasswordUtil;
import com.bookstore.domain.dto.auth.LoginDTO;
import com.bookstore.domain.dto.auth.RefreshTokenDTO;
import com.bookstore.domain.dto.auth.RegisterDTO;
import com.bookstore.domain.po.User;
import com.bookstore.domain.vo.auth.TokenVO;
import com.bookstore.domain.vo.auth.UserBriefVO;
import com.bookstore.mapper.UserMapper;
import com.bookstore.service.auth.AuthService;
import com.bookstore.service.auth.JwtUtil;
import com.bookstore.service.auth.TokenBlacklistService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final TokenBlacklistService blacklist;

    @Override
    @Transactional
    public TokenVO register(RegisterDTO dto) {
        // 1. 用户名唯一性
        Long usernameCount = userMapper.selectCount(
            new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername())
        );
        if (usernameCount > 0) {
            throw new BusinessException(ResultCode.USERNAME_TAKEN);
        }

        // 2. 手机号唯一性
        Long phoneCount = userMapper.selectCount(
            new LambdaQueryWrapper<User>().eq(User::getPhone, dto.getPhone())
        );
        if (phoneCount > 0) {
            throw new BusinessException(ResultCode.PHONE_TAKEN);
        }

        // 3. 落库
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPhone(dto.getPhone());
        user.setPasswordHash(PasswordUtil.encode(dto.getPassword()));
        user.setNickname(dto.getUsername());  // 默认昵称=用户名
        user.setRole("USER");
        user.setStatus(1);
        userMapper.insert(user);

        // 4. 直接发 token (自动登录)
        return buildTokenVO(user);
    }

    @Override
    public TokenVO login(LoginDTO dto) {
        throw new UnsupportedOperationException("login 在 Task 26 实现");
    }

    @Override
    public TokenVO refresh(RefreshTokenDTO dto) {
        throw new UnsupportedOperationException("refresh 在 Task 27 实现");
    }

    @Override
    public void logout(String accessToken) {
        throw new UnsupportedOperationException("logout 在 Task 28 实现");
    }

    private TokenVO buildTokenVO(User user) {
        String access = jwtUtil.issueAccessToken(user.getId(), user.getUsername(), user.getRole());
        String refresh = jwtUtil.issueRefreshToken(user.getId(), user.getUsername(), user.getRole());
        UserBriefVO brief = new UserBriefVO();
        brief.setId(user.getId());
        brief.setUsername(user.getUsername());
        brief.setNickname(user.getNickname());
        brief.setRole(user.getRole());
        return new TokenVO(access, refresh, jwtUtil.getAccessTtlSeconds(), brief);
    }
}
```

- [ ] **Step 25.3: 在 `ResultCode` 里补两个错误码**

打开 `bookstore-common/src/main/java/com/bookstore/common/result/ResultCode.java`,在 `INVALID_TOKEN` 后追加:

```java
USERNAME_TAKEN(1010, "用户名已被注册"),
PHONE_TAKEN(1011, "手机号已被注册"),
ACCOUNT_NOT_FOUND(1012, "账号不存在"),
PASSWORD_WRONG(1013, "密码错误"),
ACCOUNT_DISABLED(1014, "账号已禁用"),
TOKEN_TYPE_MISMATCH(1015, "Token 类型不正确"),
```

- [ ] **Step 25.4: 在 JwtUtil 里补一个工具方法**

打开 Task 18 写的 `JwtUtil`,加一个 `getAccessTtlSeconds()`:

```java
public long getAccessTtlSeconds() {
    return accessTtl.getSeconds();
}
```

- [ ] **Step 25.5: 写失败的单测(用 Mockito 隔离 Mapper)**

```java
package com.bookstore.service.auth;

import com.bookstore.common.exception.BusinessException;
import com.bookstore.common.result.ResultCode;
import com.bookstore.domain.dto.auth.RegisterDTO;
import com.bookstore.domain.po.User;
import com.bookstore.domain.vo.auth.TokenVO;
import com.bookstore.mapper.UserMapper;
import com.bookstore.service.auth.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceRegisterTest {

    @Mock UserMapper userMapper;
    @Mock TokenBlacklistService blacklist;

    JwtUtil jwtUtil;

    @InjectMocks AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        // 真实 JwtUtil,不 mock,这样能拿到真 token
        jwtUtil = new JwtUtil(
            "test-secret-must-be-long-enough-for-hs256-test-secret-must-be-long",
            Duration.ofHours(2),
            Duration.ofDays(7)
        );
        authService = new AuthServiceImpl(userMapper, jwtUtil, blacklist);
    }

    private RegisterDTO sampleDto() {
        RegisterDTO dto = new RegisterDTO();
        dto.setUsername("alice");
        dto.setPassword("hello123");
        dto.setPhone("13800000001");
        return dto;
    }

    @Test
    void register_succeeds_and_returns_tokens() {
        when(userMapper.selectCount(any())).thenReturn(0L);
        when(userMapper.insert(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(42L);
            return 1;
        });

        TokenVO vo = authService.register(sampleDto());

        assertThat(vo.getAccessToken()).isNotBlank();
        assertThat(vo.getRefreshToken()).isNotBlank();
        assertThat(vo.getUser().getId()).isEqualTo(42L);
        assertThat(vo.getUser().getUsername()).isEqualTo("alice");
        assertThat(vo.getUser().getRole()).isEqualTo("USER");
    }

    @Test
    void register_fails_when_username_taken() {
        when(userMapper.selectCount(any())).thenReturn(1L);  // 任何查询都说"已存在"

        assertThatThrownBy(() -> authService.register(sampleDto()))
            .isInstanceOf(BusinessException.class)
            .extracting("code").isEqualTo(ResultCode.USERNAME_TAKEN.getCode());
    }
}
```

- [ ] **Step 25.6: 跑单测,看到 register_succeeds_and_returns_tokens 通过, register_fails_when_username_taken 通过**

```bash
mvn -pl bookstore-service -am test -Dtest=AuthServiceRegisterTest
```

期望:2 个测试全 PASS。

> **如果失败**:常见问题是 `JwtUtil` 构造参数顺序对不上 Task 18 — 回去对照修正。

- [ ] **Step 25.7: Commit**

```bash
git add .
git commit -m "feat(auth): AuthService.register with unit tests"
```

---

### Task 26: AuthService.login

**Files:**
- Modify: `bookstore-service/src/main/java/com/bookstore/service/auth/impl/AuthServiceImpl.java` (替换 login 方法体)
- Create: `bookstore-service/src/test/java/com/bookstore/service/auth/AuthServiceLoginTest.java`

login 逻辑:
1. 用 account 同时匹配 `username` 或 `phone`
2. 找不到 → `ACCOUNT_NOT_FOUND`
3. `status == 0` → `ACCOUNT_DISABLED`
4. 密码不对 → `PASSWORD_WRONG`
5. 全过 → 发 token

- [ ] **Step 26.1: 写失败的单测(先写测试)**

```java
package com.bookstore.service.auth;

import com.bookstore.common.exception.BusinessException;
import com.bookstore.common.result.ResultCode;
import com.bookstore.common.util.PasswordUtil;
import com.bookstore.domain.dto.auth.LoginDTO;
import com.bookstore.domain.po.User;
import com.bookstore.domain.vo.auth.TokenVO;
import com.bookstore.mapper.UserMapper;
import com.bookstore.service.auth.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceLoginTest {

    @Mock UserMapper userMapper;
    @Mock TokenBlacklistService blacklist;

    AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        JwtUtil jwtUtil = new JwtUtil(
            "test-secret-must-be-long-enough-for-hs256-test-secret-must-be-long",
            Duration.ofHours(2),
            Duration.ofDays(7)
        );
        authService = new AuthServiceImpl(userMapper, jwtUtil, blacklist);
    }

    private User existingUser() {
        User u = new User();
        u.setId(1L);
        u.setUsername("alice");
        u.setPhone("13800000001");
        u.setPasswordHash(PasswordUtil.encode("hello123"));
        u.setRole("USER");
        u.setStatus(1);
        u.setNickname("Alice");
        return u;
    }

    @Test
    void login_by_username_succeeds() {
        when(userMapper.selectOne(any())).thenReturn(existingUser());

        LoginDTO dto = new LoginDTO();
        dto.setAccount("alice");
        dto.setPassword("hello123");

        TokenVO vo = authService.login(dto);
        assertThat(vo.getAccessToken()).isNotBlank();
        assertThat(vo.getUser().getUsername()).isEqualTo("alice");
    }

    @Test
    void login_account_not_found() {
        when(userMapper.selectOne(any())).thenReturn(null);

        LoginDTO dto = new LoginDTO();
        dto.setAccount("ghost");
        dto.setPassword("xxxxxx");

        assertThatThrownBy(() -> authService.login(dto))
            .isInstanceOf(BusinessException.class)
            .extracting("code").isEqualTo(ResultCode.ACCOUNT_NOT_FOUND.getCode());
    }

    @Test
    void login_password_wrong() {
        when(userMapper.selectOne(any())).thenReturn(existingUser());

        LoginDTO dto = new LoginDTO();
        dto.setAccount("alice");
        dto.setPassword("WRONG-PASSWORD");

        assertThatThrownBy(() -> authService.login(dto))
            .isInstanceOf(BusinessException.class)
            .extracting("code").isEqualTo(ResultCode.PASSWORD_WRONG.getCode());
    }

    @Test
    void login_account_disabled() {
        User u = existingUser();
        u.setStatus(0);
        when(userMapper.selectOne(any())).thenReturn(u);

        LoginDTO dto = new LoginDTO();
        dto.setAccount("alice");
        dto.setPassword("hello123");

        assertThatThrownBy(() -> authService.login(dto))
            .isInstanceOf(BusinessException.class)
            .extracting("code").isEqualTo(ResultCode.ACCOUNT_DISABLED.getCode());
    }
}
```

- [ ] **Step 26.2: 跑测试,看到全部 4 个 FAIL(login 还没实现)**

```bash
mvn -pl bookstore-service -am test -Dtest=AuthServiceLoginTest
```

期望:4 个 FAIL,错误信息是 `UnsupportedOperationException: login 在 Task 26 实现`。

- [ ] **Step 26.3: 实现 login**

打开 `AuthServiceImpl`,替换 login 方法:

```java
@Override
public TokenVO login(LoginDTO dto) {
    // 用 account 同时匹配 username 或 phone
    User user = userMapper.selectOne(
        new LambdaQueryWrapper<User>()
            .and(w -> w.eq(User::getUsername, dto.getAccount())
                       .or()
                       .eq(User::getPhone, dto.getAccount()))
    );
    if (user == null) {
        throw new BusinessException(ResultCode.ACCOUNT_NOT_FOUND);
    }
    if (user.getStatus() == null || user.getStatus() == 0) {
        throw new BusinessException(ResultCode.ACCOUNT_DISABLED);
    }
    if (!PasswordUtil.matches(dto.getPassword(), user.getPasswordHash())) {
        throw new BusinessException(ResultCode.PASSWORD_WRONG);
    }
    return buildTokenVO(user);
}
```

- [ ] **Step 26.4: 跑测试,看到全部 4 个 PASS**

```bash
mvn -pl bookstore-service -am test -Dtest=AuthServiceLoginTest
```

- [ ] **Step 26.5: Commit**

```bash
git add .
git commit -m "feat(auth): AuthService.login with username/phone match"
```

---

### Task 27: AuthService.refresh

**Files:**
- Modify: `bookstore-service/src/main/java/com/bookstore/service/auth/impl/AuthServiceImpl.java` (替换 refresh 方法体)
- Create: `bookstore-service/src/test/java/com/bookstore/service/auth/AuthServiceRefreshTest.java`

refresh 逻辑:
1. `parse` refresh token,解析失败 → `INVALID_TOKEN`
2. 检查 type claim 必须是 `refresh`,否则 `TOKEN_TYPE_MISMATCH`
3. (可选)查 blacklist;refresh token 也支持登出后失效
4. 用 claims 里的 userId 重新查 user,确认还存在且 status=1
5. 发**新的 access + 新的 refresh**(refresh 也滚动,旧的 refresh 加入黑名单)

> **设计权衡**:也可以让 refresh 不滚动(只换 access)。本项目选滚动方案,因为它更安全(refresh 被偷也只能用一次)。

- [ ] **Step 27.1: JwtUtil 补一个 parse 方法返回完整 claims**

打开 Task 18 写的 `JwtUtil`,如果还没有 `parse(String)` 方法,加上:

```java
public UserClaims parse(String token) {
    try {
        Claims claims = Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .getPayload();
        return new UserClaims(
            Long.parseLong(claims.getSubject()),
            claims.get("username", String.class),
            claims.get("role", String.class),
            claims.getId(),
            claims.getExpiration().getTime()
        );
    } catch (io.jsonwebtoken.JwtException | IllegalArgumentException e) {
        throw new com.bookstore.common.exception.AuthException(
            com.bookstore.common.result.ResultCode.INVALID_TOKEN);
    }
}

/** 单独取 type claim,用于区分 access / refresh */
public String getTokenType(String token) {
    return Jwts.parser()
        .verifyWith(key)
        .build()
        .parseSignedClaims(token)
        .getPayload()
        .get("type", String.class);
}
```

- [ ] **Step 27.2: 写失败的测试**

```java
package com.bookstore.service.auth;

import com.bookstore.common.exception.AuthException;
import com.bookstore.common.exception.BusinessException;
import com.bookstore.common.result.ResultCode;
import com.bookstore.domain.dto.auth.RefreshTokenDTO;
import com.bookstore.domain.po.User;
import com.bookstore.domain.vo.auth.TokenVO;
import com.bookstore.mapper.UserMapper;
import com.bookstore.service.auth.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceRefreshTest {

    @Mock UserMapper userMapper;
    @Mock TokenBlacklistService blacklist;

    JwtUtil jwtUtil;
    AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(
            "test-secret-must-be-long-enough-for-hs256-test-secret-must-be-long",
            Duration.ofHours(2),
            Duration.ofDays(7)
        );
        authService = new AuthServiceImpl(userMapper, jwtUtil, blacklist);
    }

    @Test
    void refresh_with_valid_refresh_token_returns_new_pair() {
        User u = new User();
        u.setId(1L); u.setUsername("alice"); u.setRole("USER"); u.setStatus(1);
        u.setNickname("Alice");
        when(userMapper.selectById(1L)).thenReturn(u);
        when(blacklist.isRevoked(org.mockito.ArgumentMatchers.anyString())).thenReturn(false);

        String refresh = jwtUtil.issueRefreshToken(1L, "alice", "USER");
        RefreshTokenDTO dto = new RefreshTokenDTO();
        dto.setRefreshToken(refresh);

        TokenVO vo = authService.refresh(dto);
        assertThat(vo.getAccessToken()).isNotBlank();
        assertThat(vo.getRefreshToken()).isNotBlank().isNotEqualTo(refresh);  // 滚动
    }

    @Test
    void refresh_rejects_access_token() {
        String access = jwtUtil.issueAccessToken(1L, "alice", "USER");
        RefreshTokenDTO dto = new RefreshTokenDTO();
        dto.setRefreshToken(access);  // 错误地传了 access token

        assertThatThrownBy(() -> authService.refresh(dto))
            .isInstanceOf(BusinessException.class)
            .extracting("code").isEqualTo(ResultCode.TOKEN_TYPE_MISMATCH.getCode());
    }

    @Test
    void refresh_rejects_invalid_token() {
        RefreshTokenDTO dto = new RefreshTokenDTO();
        dto.setRefreshToken("not-a-real-jwt");

        assertThatThrownBy(() -> authService.refresh(dto))
            .isInstanceOf(AuthException.class)
            .extracting("code").isEqualTo(ResultCode.INVALID_TOKEN.getCode());
    }
}
```

- [ ] **Step 27.3: 跑测试,看到 3 个 FAIL**

```bash
mvn -pl bookstore-service -am test -Dtest=AuthServiceRefreshTest
```

- [ ] **Step 27.4: 实现 refresh**

替换 `AuthServiceImpl.refresh`:

```java
@Override
public TokenVO refresh(RefreshTokenDTO dto) {
    // 1. 解析(失败抛 AuthException)
    JwtUtil.UserClaims claims = jwtUtil.parse(dto.getRefreshToken());

    // 2. 类型必须是 refresh
    String type = jwtUtil.getTokenType(dto.getRefreshToken());
    if (!"refresh".equals(type)) {
        throw new BusinessException(ResultCode.TOKEN_TYPE_MISMATCH);
    }

    // 3. 黑名单
    if (blacklist.isRevoked(claims.jti())) {
        throw new BusinessException(ResultCode.INVALID_TOKEN);
    }

    // 4. 用户还存在?状态正常?
    User user = userMapper.selectById(claims.userId());
    if (user == null) {
        throw new BusinessException(ResultCode.ACCOUNT_NOT_FOUND);
    }
    if (user.getStatus() == null || user.getStatus() == 0) {
        throw new BusinessException(ResultCode.ACCOUNT_DISABLED);
    }

    // 5. 旧 refresh 加黑名单(滚动) — TokenBlacklistService.revoke 内部会算 TTL
    blacklist.revoke(claims.jti(), claims.expiresAtEpochMs());

    // 6. 发新对
    return buildTokenVO(user);
}
```

- [ ] **Step 27.5: 跑测试,看到 3 个 PASS**

```bash
mvn -pl bookstore-service -am test -Dtest=AuthServiceRefreshTest
```

- [ ] **Step 27.6: Commit**

```bash
git add .
git commit -m "feat(auth): rolling refresh-token flow"
```

---

### Task 28: AuthService.logout + AuthController

**Files:**
- Modify: `bookstore-service/src/main/java/com/bookstore/service/auth/impl/AuthServiceImpl.java` (实现 logout)
- Create: `bookstore-web-api/src/main/java/com/bookstore/api/auth/AuthController.java`
- Create: `bookstore-web-api/src/test/java/com/bookstore/api/auth/AuthControllerSliceTest.java`

logout 不再单写 service 单测(逻辑只有"加黑名单"),直接做切片测试覆盖整个 controller→service 链路。

- [ ] **Step 28.1: 实现 logout**

替换 `AuthServiceImpl.logout`:

```java
@Override
public void logout(String accessToken) {
    if (accessToken == null || accessToken.isBlank()) {
        return;  // 没 token 也算登出成功(幂等)
    }
    try {
        JwtUtil.UserClaims claims = jwtUtil.parse(accessToken);
        // TokenBlacklistService.revoke 自己会判断 TTL <= 0 的边界
        blacklist.revoke(claims.jti(), claims.expiresAtEpochMs());
    } catch (Exception ignore) {
        // token 无效就当已经登出了
    }
}
```

- [ ] **Step 28.2: 写 AuthController**

```java
package com.bookstore.api.auth;

import com.bookstore.common.result.Result;
import com.bookstore.domain.dto.auth.LoginDTO;
import com.bookstore.domain.dto.auth.RefreshTokenDTO;
import com.bookstore.domain.dto.auth.RegisterDTO;
import com.bookstore.domain.vo.auth.TokenVO;
import com.bookstore.service.auth.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public Result<TokenVO> register(@Valid @RequestBody RegisterDTO dto) {
        return Result.ok(authService.register(dto));
    }

    @PostMapping("/login")
    public Result<TokenVO> login(@Valid @RequestBody LoginDTO dto) {
        return Result.ok(authService.login(dto));
    }

    @PostMapping("/refresh")
    public Result<TokenVO> refresh(@Valid @RequestBody RefreshTokenDTO dto) {
        return Result.ok(authService.refresh(dto));
    }

    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest request) {
        String token = extractBearer(request);
        authService.logout(token);
        return Result.ok();
    }

    private String extractBearer(HttpServletRequest request) {
        String h = request.getHeader("Authorization");
        if (h != null && h.startsWith("Bearer ")) {
            return h.substring(7);
        }
        return null;
    }
}
```

- [ ] **Step 28.3: 切片测试(MockBean 隔离 service,只测 web 层)**

```java
package com.bookstore.api.auth;

import com.bookstore.domain.dto.auth.LoginDTO;
import com.bookstore.domain.dto.auth.RegisterDTO;
import com.bookstore.domain.vo.auth.TokenVO;
import com.bookstore.domain.vo.auth.UserBriefVO;
import com.bookstore.service.auth.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
class AuthControllerSliceTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;
    @MockBean AuthService authService;

    private TokenVO sampleTokenVO() {
        UserBriefVO brief = new UserBriefVO();
        brief.setId(1L); brief.setUsername("alice"); brief.setRole("USER");
        return new TokenVO("acc", "ref", 7200L, brief);
    }

    @Test
    void register_returns_200_with_token() throws Exception {
        when(authService.register(any())).thenReturn(sampleTokenVO());

        RegisterDTO dto = new RegisterDTO();
        dto.setUsername("alice"); dto.setPassword("hello123"); dto.setPhone("13800000001");

        mvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(dto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.accessToken").value("acc"))
            .andExpect(jsonPath("$.data.refreshToken").value("ref"))
            .andExpect(jsonPath("$.data.user.username").value("alice"));
    }

    @Test
    void register_validation_fails_when_username_too_short() throws Exception {
        RegisterDTO dto = new RegisterDTO();
        dto.setUsername("ab");  // 太短
        dto.setPassword("hello123");
        dto.setPhone("13800000001");

        mvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(dto)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void login_returns_200_with_token() throws Exception {
        when(authService.login(any())).thenReturn(sampleTokenVO());

        LoginDTO dto = new LoginDTO();
        dto.setAccount("alice"); dto.setPassword("hello123");

        mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(dto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.accessToken").value("acc"));
    }

    @Test
    void logout_extracts_bearer_and_returns_200() throws Exception {
        mvc.perform(post("/api/auth/logout")
                .header("Authorization", "Bearer some-token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));
    }
}
```

- [ ] **Step 28.4: WebMvcConfig 排除 /api/auth/** 不走 LoginInterceptor**

回头打开 Task 15 的 `WebMvcConfig`,确认 `/api/auth/**` 在 `excludePathPatterns` 里。如果没有,加上:

```java
.excludePathPatterns(
    "/api/auth/**",       // 注册/登录/刷新无需登录;logout 自己处理 token
    "/error",
    "/actuator/**"
)
```

- [ ] **Step 28.5: 跑切片测试**

```bash
mvn -pl bookstore-web-api -am test -Dtest=AuthControllerSliceTest
```

期望:4 个测试全 PASS。

> **常见问题**:`@WebMvcTest` 默认不扫描 `@Configuration` 类,如果切片启动失败说找不到某个 bean,可在测试类上加 `@Import(GlobalExceptionHandler.class)` 或显式声明所需的最小 bean。

- [ ] **Step 28.6: 手工冒烟一次完整链路(本地起 docker-compose 之后)**

```bash
docker compose up -d
mvn -pl bookstore-web-api spring-boot:run
```

另开终端,用 curl 走一遍:

```bash
# 注册
curl -X POST http://localhost:8080/api/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"username":"alice","password":"hello123","phone":"13800000001"}'

# 登录
curl -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"account":"alice","password":"hello123"}'

# 用返回的 accessToken 调一个需要登录的接口(Phase 8 加完 /api/user/me 再回头测)
```

- [ ] **Step 28.7: Commit**

```bash
git add .
git commit -m "feat(auth): logout + AuthController with slice tests"
```

---

## Phase 8: 用户模块 (个人资料)

**目标:** 用户登录后可以查看/修改自己的资料,改密码,换头像。所有接口都依赖 `UserContext` 拿当前 userId — 这正是 Phase 4 写的 ThreadLocal 派上用场的地方。

**端点:**
- `GET /api/user/me` — 获取当前用户资料
- `PUT /api/user/me` — 修改昵称/性别/生日
- `PUT /api/user/me/password` — 改密码(老密码校验 + bcrypt 新密码)
- `PUT /api/user/me/avatar` — 更新头像(只接收 OSS key,真正的上传走前端直传 OSS,Phase 暂不实现签名)

---

### Task 29: UserService.getProfile

**Files:**
- Create: `bookstore-domain/src/main/java/com/bookstore/domain/vo/user/UserProfileVO.java`
- Create: `bookstore-service/src/main/java/com/bookstore/service/user/UserService.java`
- Create: `bookstore-service/src/main/java/com/bookstore/service/user/impl/UserServiceImpl.java`
- Create: `bookstore-service/src/test/java/com/bookstore/service/user/UserServiceProfileTest.java`

- [ ] **Step 29.1: UserProfileVO**

```java
package com.bookstore.domain.vo.user;

import lombok.Data;

import java.time.LocalDate;

@Data
public class UserProfileVO {

    private Long id;
    private String username;
    private String phone;       // 脱敏:138****0001
    private String email;
    private String nickname;
    private String avatarUrl;
    private Integer gender;
    private LocalDate birthday;
    private String role;
}
```

- [ ] **Step 29.2: UserService 接口**

```java
package com.bookstore.service.user;

import com.bookstore.domain.dto.user.ChangePasswordDTO;
import com.bookstore.domain.dto.user.UpdateProfileDTO;
import com.bookstore.domain.vo.user.UserProfileVO;

public interface UserService {

    UserProfileVO getProfile(Long userId);

    UserProfileVO updateProfile(Long userId, UpdateProfileDTO dto);

    void changePassword(Long userId, ChangePasswordDTO dto);

    UserProfileVO updateAvatar(Long userId, String avatarKey);
}
```

> 这里前向引用了 `UpdateProfileDTO` 和 `ChangePasswordDTO`,下个 Task 立即定义。

- [ ] **Step 29.3: UserServiceImpl 骨架(只实现 getProfile)**

```java
package com.bookstore.service.user.impl;

import com.bookstore.common.exception.BusinessException;
import com.bookstore.common.result.ResultCode;
import com.bookstore.domain.dto.user.ChangePasswordDTO;
import com.bookstore.domain.dto.user.UpdateProfileDTO;
import com.bookstore.domain.po.User;
import com.bookstore.domain.vo.user.UserProfileVO;
import com.bookstore.mapper.UserMapper;
import com.bookstore.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;

    /** OSS 公网访问前缀,例如 https://bookstore.oss-cn-hangzhou.aliyuncs.com/ */
    @Value("${bookstore.oss.public-prefix:}")
    private String ossPrefix;

    @Override
    public UserProfileVO getProfile(Long userId) {
        User u = userMapper.selectById(userId);
        if (u == null) {
            throw new BusinessException(ResultCode.ACCOUNT_NOT_FOUND);
        }
        return toVO(u);
    }

    @Override
    public UserProfileVO updateProfile(Long userId, UpdateProfileDTO dto) {
        throw new UnsupportedOperationException("updateProfile 在 Task 30 实现");
    }

    @Override
    public void changePassword(Long userId, ChangePasswordDTO dto) {
        throw new UnsupportedOperationException("changePassword 在 Task 31 实现");
    }

    @Override
    public UserProfileVO updateAvatar(Long userId, String avatarKey) {
        throw new UnsupportedOperationException("updateAvatar 在 Task 32 实现");
    }

    UserProfileVO toVO(User u) {
        UserProfileVO vo = new UserProfileVO();
        vo.setId(u.getId());
        vo.setUsername(u.getUsername());
        vo.setPhone(maskPhone(u.getPhone()));
        vo.setEmail(u.getEmail());
        vo.setNickname(u.getNickname());
        vo.setAvatarUrl(u.getAvatarKey() == null ? null : ossPrefix + u.getAvatarKey());
        vo.setGender(u.getGender());
        vo.setBirthday(u.getBirthday());
        vo.setRole(u.getRole());
        return vo;
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() != 11) return phone;
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }
}
```

- [ ] **Step 29.4: 单测**

```java
package com.bookstore.service.user;

import com.bookstore.common.exception.BusinessException;
import com.bookstore.domain.po.User;
import com.bookstore.domain.vo.user.UserProfileVO;
import com.bookstore.mapper.UserMapper;
import com.bookstore.service.user.impl.UserServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceProfileTest {

    @Mock UserMapper userMapper;
    @InjectMocks UserServiceImpl userService;

    @Test
    void getProfile_returns_masked_phone_and_full_avatar_url() {
        ReflectionTestUtils.setField(userService, "ossPrefix", "https://oss.example.com/");

        User u = new User();
        u.setId(1L);
        u.setUsername("alice");
        u.setPhone("13812345678");
        u.setNickname("Alice");
        u.setAvatarKey("avatars/1.jpg");
        u.setRole("USER");
        when(userMapper.selectById(1L)).thenReturn(u);

        UserProfileVO vo = userService.getProfile(1L);

        assertThat(vo.getPhone()).isEqualTo("138****5678");
        assertThat(vo.getAvatarUrl()).isEqualTo("https://oss.example.com/avatars/1.jpg");
        assertThat(vo.getUsername()).isEqualTo("alice");
    }

    @Test
    void getProfile_throws_when_not_found() {
        when(userMapper.selectById(99L)).thenReturn(null);
        assertThatThrownBy(() -> userService.getProfile(99L))
            .isInstanceOf(BusinessException.class);
    }
}
```

- [ ] **Step 29.5: 跑测试**

```bash
mvn -pl bookstore-service -am test -Dtest=UserServiceProfileTest
```

- [ ] **Step 29.6: Commit**

```bash
git add .
git commit -m "feat(user): UserService.getProfile with phone masking"
```

---

### Task 30: UserService.updateProfile

**Files:**
- Create: `bookstore-domain/src/main/java/com/bookstore/domain/dto/user/UpdateProfileDTO.java`
- Modify: `bookstore-service/src/main/java/com/bookstore/service/user/impl/UserServiceImpl.java` (替换 updateProfile)
- Create: `bookstore-service/src/test/java/com/bookstore/service/user/UserServiceUpdateProfileTest.java`

只允许改:nickname / gender / birthday。username/phone/role/status 不能由用户自己改。

- [ ] **Step 30.1: UpdateProfileDTO**

```java
package com.bookstore.domain.dto.user;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdateProfileDTO {

    @Size(max = 30, message = "昵称不能超过 30 字")
    private String nickname;

    @Min(value = 0, message = "gender 仅 0/1/2")
    @Max(value = 2, message = "gender 仅 0/1/2")
    private Integer gender;

    private LocalDate birthday;
}
```

- [ ] **Step 30.2: 写失败的单测**

```java
package com.bookstore.service.user;

import com.bookstore.domain.dto.user.UpdateProfileDTO;
import com.bookstore.domain.po.User;
import com.bookstore.domain.vo.user.UserProfileVO;
import com.bookstore.mapper.UserMapper;
import com.bookstore.service.user.impl.UserServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceUpdateProfileTest {

    @Mock UserMapper userMapper;
    @InjectMocks UserServiceImpl userService;

    @Test
    void updateProfile_only_writes_allowed_fields() {
        User existing = new User();
        existing.setId(1L);
        existing.setUsername("alice");
        existing.setPhone("13812345678");
        existing.setNickname("Old");
        existing.setRole("USER");
        when(userMapper.selectById(1L)).thenReturn(existing);

        UpdateProfileDTO dto = new UpdateProfileDTO();
        dto.setNickname("New Alice");
        dto.setGender(1);
        dto.setBirthday(LocalDate.of(2000, 1, 1));

        UserProfileVO vo = userService.updateProfile(1L, dto);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).updateById(captor.capture());
        User updated = captor.getValue();

        assertThat(updated.getNickname()).isEqualTo("New Alice");
        assertThat(updated.getGender()).isEqualTo(1);
        assertThat(updated.getBirthday()).isEqualTo(LocalDate.of(2000, 1, 1));
        // username/phone 没改
        assertThat(updated.getUsername()).isNull();  // 只 set 了允许的字段,没改的字段在 PO 上是 null,MyBatis-Plus 默认不会更新 null
        assertThat(updated.getPhone()).isNull();

        assertThat(vo.getNickname()).isEqualTo("New Alice");
    }
}
```

> **设计点**:用一个空白的 `User` 对象只 set 允许字段,然后 `updateById`,这样自动避开了用户篡改 phone/role 的攻击面。MyBatis-Plus 默认 `FieldStrategy.NOT_NULL`,null 字段不会写入 SQL。

- [ ] **Step 30.3: 跑测试,看到 FAIL(updateProfile 还没实现)**

```bash
mvn -pl bookstore-service -am test -Dtest=UserServiceUpdateProfileTest
```

- [ ] **Step 30.4: 实现**

替换 `UserServiceImpl.updateProfile`:

```java
@Override
public UserProfileVO updateProfile(Long userId, UpdateProfileDTO dto) {
    User existing = userMapper.selectById(userId);
    if (existing == null) {
        throw new BusinessException(ResultCode.ACCOUNT_NOT_FOUND);
    }

    User patch = new User();
    patch.setId(userId);
    patch.setNickname(dto.getNickname());
    patch.setGender(dto.getGender());
    patch.setBirthday(dto.getBirthday());
    userMapper.updateById(patch);

    // 把改动反映到原对象上,直接用它生成 VO
    if (dto.getNickname() != null) existing.setNickname(dto.getNickname());
    if (dto.getGender() != null) existing.setGender(dto.getGender());
    if (dto.getBirthday() != null) existing.setBirthday(dto.getBirthday());
    return toVO(existing);
}
```

- [ ] **Step 30.5: 跑测试,PASS**

```bash
mvn -pl bookstore-service -am test -Dtest=UserServiceUpdateProfileTest
```

- [ ] **Step 30.6: Commit**

```bash
git add .
git commit -m "feat(user): updateProfile with whitelist fields"
```

---

### Task 31: UserService.changePassword

**Files:**
- Create: `bookstore-domain/src/main/java/com/bookstore/domain/dto/user/ChangePasswordDTO.java`
- Modify: `bookstore-service/src/main/java/com/bookstore/service/user/impl/UserServiceImpl.java` (替换 changePassword)
- Create: `bookstore-service/src/test/java/com/bookstore/service/user/UserServiceChangePasswordTest.java`

逻辑:
1. 校验老密码
2. 新密码不能跟老密码完全相同
3. bcrypt 后写库

> 改密成功后**不强制下线**(简化设计;真实生产建议清掉所有 refresh token)。可在答辩"未来迭代"里提一句。

- [ ] **Step 31.1: ChangePasswordDTO**

```java
package com.bookstore.domain.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChangePasswordDTO {

    @NotBlank
    private String oldPassword;

    @NotBlank
    @Size(min = 6, max = 32, message = "密码长度 6-32")
    private String newPassword;
}
```

- [ ] **Step 31.2: 写失败的单测**

```java
package com.bookstore.service.user;

import com.bookstore.common.exception.BusinessException;
import com.bookstore.common.result.ResultCode;
import com.bookstore.common.util.PasswordUtil;
import com.bookstore.domain.dto.user.ChangePasswordDTO;
import com.bookstore.domain.po.User;
import com.bookstore.mapper.UserMapper;
import com.bookstore.service.user.impl.UserServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceChangePasswordTest {

    @Mock UserMapper userMapper;
    @InjectMocks UserServiceImpl userService;

    private User userWithPassword(String raw) {
        User u = new User();
        u.setId(1L);
        u.setUsername("alice");
        u.setPasswordHash(PasswordUtil.encode(raw));
        u.setStatus(1);
        return u;
    }

    @Test
    void changePassword_succeeds() {
        when(userMapper.selectById(1L)).thenReturn(userWithPassword("oldpass"));

        ChangePasswordDTO dto = new ChangePasswordDTO();
        dto.setOldPassword("oldpass");
        dto.setNewPassword("newpass");

        userService.changePassword(1L, dto);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).updateById(captor.capture());
        String storedHash = captor.getValue().getPasswordHash();
        assertThat(PasswordUtil.matches("newpass", storedHash)).isTrue();
    }

    @Test
    void changePassword_rejects_wrong_old() {
        when(userMapper.selectById(1L)).thenReturn(userWithPassword("oldpass"));

        ChangePasswordDTO dto = new ChangePasswordDTO();
        dto.setOldPassword("WRONG");
        dto.setNewPassword("newpass");

        assertThatThrownBy(() -> userService.changePassword(1L, dto))
            .isInstanceOf(BusinessException.class)
            .extracting("code").isEqualTo(ResultCode.PASSWORD_WRONG.getCode());
    }

    @Test
    void changePassword_rejects_same_password() {
        when(userMapper.selectById(1L)).thenReturn(userWithPassword("oldpass"));

        ChangePasswordDTO dto = new ChangePasswordDTO();
        dto.setOldPassword("oldpass");
        dto.setNewPassword("oldpass");

        assertThatThrownBy(() -> userService.changePassword(1L, dto))
            .isInstanceOf(BusinessException.class);
    }
}
```

- [ ] **Step 31.3: 在 ResultCode 里追加一个错误码**

打开 `ResultCode`,加:

```java
NEW_PASSWORD_SAME_AS_OLD(1016, "新密码与原密码相同"),
```

- [ ] **Step 31.4: 实现**

替换 `UserServiceImpl.changePassword`:

```java
@Override
public void changePassword(Long userId, ChangePasswordDTO dto) {
    User u = userMapper.selectById(userId);
    if (u == null) {
        throw new BusinessException(ResultCode.ACCOUNT_NOT_FOUND);
    }
    if (!PasswordUtil.matches(dto.getOldPassword(), u.getPasswordHash())) {
        throw new BusinessException(ResultCode.PASSWORD_WRONG);
    }
    if (dto.getOldPassword().equals(dto.getNewPassword())) {
        throw new BusinessException(ResultCode.NEW_PASSWORD_SAME_AS_OLD);
    }

    User patch = new User();
    patch.setId(userId);
    patch.setPasswordHash(PasswordUtil.encode(dto.getNewPassword()));
    userMapper.updateById(patch);
}
```

- [ ] **Step 31.5: 跑测试**

```bash
mvn -pl bookstore-service -am test -Dtest=UserServiceChangePasswordTest
```

期望:3 个 PASS。

- [ ] **Step 31.6: Commit**

```bash
git add .
git commit -m "feat(user): changePassword with old-password verification"
```

---

### Task 32: updateAvatar + UserController

**Files:**
- Modify: `bookstore-service/src/main/java/com/bookstore/service/user/impl/UserServiceImpl.java` (实现 updateAvatar)
- Create: `bookstore-domain/src/main/java/com/bookstore/domain/dto/user/UpdateAvatarDTO.java`
- Create: `bookstore-web-api/src/main/java/com/bookstore/api/user/UserController.java`
- Create: `bookstore-web-api/src/test/java/com/bookstore/api/user/UserControllerSliceTest.java`

avatarKey 是前端直传 OSS 后拿到的对象 key,后端只需校验格式(`avatars/` 前缀 + 不超过 200 字符)和写库。

- [ ] **Step 32.1: UpdateAvatarDTO**

```java
package com.bookstore.domain.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateAvatarDTO {

    @NotBlank
    @Size(max = 200)
    @Pattern(regexp = "^avatars/.+", message = "avatarKey 必须以 avatars/ 开头")
    private String avatarKey;
}
```

- [ ] **Step 32.2: 实现 updateAvatar**

替换 `UserServiceImpl.updateAvatar`:

```java
@Override
public UserProfileVO updateAvatar(Long userId, String avatarKey) {
    User existing = userMapper.selectById(userId);
    if (existing == null) {
        throw new BusinessException(ResultCode.ACCOUNT_NOT_FOUND);
    }
    User patch = new User();
    patch.setId(userId);
    patch.setAvatarKey(avatarKey);
    userMapper.updateById(patch);

    existing.setAvatarKey(avatarKey);
    return toVO(existing);
}
```

- [ ] **Step 32.3: UserController**

```java
package com.bookstore.api.user;

import com.bookstore.common.annotation.LoginRequired;
import com.bookstore.common.context.UserContext;
import com.bookstore.common.result.Result;
import com.bookstore.domain.dto.user.ChangePasswordDTO;
import com.bookstore.domain.dto.user.UpdateAvatarDTO;
import com.bookstore.domain.dto.user.UpdateProfileDTO;
import com.bookstore.domain.vo.user.UserProfileVO;
import com.bookstore.service.user.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@LoginRequired
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public Result<UserProfileVO> me() {
        return Result.ok(userService.getProfile(UserContext.requireUserId()));
    }

    @PutMapping("/me")
    public Result<UserProfileVO> updateMe(@Valid @RequestBody UpdateProfileDTO dto) {
        return Result.ok(userService.updateProfile(UserContext.requireUserId(), dto));
    }

    @PutMapping("/me/password")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordDTO dto) {
        userService.changePassword(UserContext.requireUserId(), dto);
        return Result.ok();
    }

    @PutMapping("/me/avatar")
    public Result<UserProfileVO> updateAvatar(@Valid @RequestBody UpdateAvatarDTO dto) {
        return Result.ok(userService.updateAvatar(UserContext.requireUserId(), dto.getAvatarKey()));
    }
}
```

- [ ] **Step 32.4: 在 UserContext 里加 requireUserId 静态方法(如果还没有)**

回头打开 Task 11 的 `UserContext`,确认有:

```java
public static Long requireUserId() {
    Long id = getUserId();
    if (id == null) {
        throw new com.bookstore.common.exception.AuthException(
            com.bookstore.common.result.ResultCode.UNAUTHORIZED);
    }
    return id;
}
```

> 没有的话,加上;Phase 4 应该已经写过了,这步只是双重检查。

- [ ] **Step 32.5: 切片测试**

```java
package com.bookstore.api.user;

import com.bookstore.common.context.UserContext;
import com.bookstore.domain.dto.user.UpdateAvatarDTO;
import com.bookstore.domain.dto.user.UpdateProfileDTO;
import com.bookstore.domain.vo.user.UserProfileVO;
import com.bookstore.service.user.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = UserController.class,
    excludeAutoConfiguration = {})
@Import(com.bookstore.common.exception.GlobalExceptionHandler.class)
class UserControllerSliceTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;
    @MockBean UserService userService;

    @BeforeEach
    void setUp() {
        UserContext.setUserId(1L);
        UserContext.setRole("USER");
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    private UserProfileVO sample() {
        UserProfileVO v = new UserProfileVO();
        v.setId(1L); v.setUsername("alice"); v.setNickname("Alice");
        v.setPhone("138****5678"); v.setRole("USER");
        return v;
    }

    @Test
    void me_returns_profile() throws Exception {
        when(userService.getProfile(1L)).thenReturn(sample());

        mvc.perform(get("/api/user/me"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.username").value("alice"))
            .andExpect(jsonPath("$.data.phone").value("138****5678"));
    }

    @Test
    void updateMe_validates_gender_range() throws Exception {
        UpdateProfileDTO dto = new UpdateProfileDTO();
        dto.setGender(5);  // 越界

        mvc.perform(put("/api/user/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(dto)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void updateAvatar_rejects_bad_prefix() throws Exception {
        UpdateAvatarDTO dto = new UpdateAvatarDTO();
        dto.setAvatarKey("../etc/passwd");  // 试图越权

        mvc.perform(put("/api/user/me/avatar")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(dto)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void updateAvatar_accepts_valid_key() throws Exception {
        when(userService.updateAvatar(eq(1L), eq("avatars/1.jpg"))).thenReturn(sample());

        UpdateAvatarDTO dto = new UpdateAvatarDTO();
        dto.setAvatarKey("avatars/1.jpg");

        mvc.perform(put("/api/user/me/avatar")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(dto)))
            .andExpect(status().isOk());
    }
}
```

> **注意**:切片测试里直接 set/clear `UserContext` 是因为 `LoginInterceptor` 在 `@WebMvcTest` 里默认不会激活(slice 不加载 `WebMvcConfig`)。这种打法只验证 controller 业务,不验证拦截链路。完整链路在 Task 39 的集成测试里覆盖。

- [ ] **Step 32.6: 跑测试**

```bash
mvn -pl bookstore-web-api -am test -Dtest=UserControllerSliceTest
```

- [ ] **Step 32.7: Commit**

```bash
git add .
git commit -m "feat(user): UserController + updateAvatar with key validation"
```

---

## Phase 9: 地址模块

**目标:** 用户的收货地址 CRUD,支持设置/切换默认地址。地址表 `address` 已经在 V1 schema 里建好(参见 Task 5)。

**端点:**
- `GET /api/address` — 列出当前用户所有地址(默认在前)
- `POST /api/address` — 新增地址
- `PUT /api/address/{id}` — 修改
- `DELETE /api/address/{id}` — 删除(软删 — 因为订单可能引用)
- `PUT /api/address/{id}/default` — 设为默认

**核心难点:** 设置默认地址必须用事务把"原默认 → 普通" + "目标 → 默认"原子化。

---

### Task 33: Address 实体 + Mapper + DTO/VO

**Files:**
- Create: `bookstore-domain/src/main/java/com/bookstore/domain/po/Address.java`
- Create: `bookstore-mapper/src/main/java/com/bookstore/mapper/AddressMapper.java`
- Create: `bookstore-domain/src/main/java/com/bookstore/domain/dto/address/AddressFormDTO.java`
- Create: `bookstore-domain/src/main/java/com/bookstore/domain/vo/address/AddressVO.java`

- [ ] **Step 33.1: Address 实体**

```java
package com.bookstore.domain.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.bookstore.domain.po.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("address")
public class Address extends BaseEntity {

    @TableField("user_id")
    private Long userId;

    private String receiver;
    private String phone;

    private String province;
    private String city;
    private String district;

    /** 详细地址,街道+门牌 */
    @TableField("detail_address")
    private String detailAddress;

    /** 0 否 / 1 是 */
    @TableField("is_default")
    private Integer isDefault;
}
```

- [ ] **Step 33.2: AddressMapper**

```java
package com.bookstore.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bookstore.domain.po.Address;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

public interface AddressMapper extends BaseMapper<Address> {

    /** 把指定用户除 keepId 外所有地址置为非默认 */
    @Update("UPDATE address SET is_default = 0 " +
            "WHERE user_id = #{userId} AND id <> #{keepId} AND deleted = 0")
    int unsetOtherDefaults(@Param("userId") Long userId,
                           @Param("keepId") Long keepId);
}
```

- [ ] **Step 33.3: AddressFormDTO**

```java
package com.bookstore.domain.dto.address;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AddressFormDTO {

    @NotBlank
    @Size(max = 30)
    private String receiver;

    @NotBlank
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @NotBlank
    @Size(max = 20)
    private String province;

    @NotBlank
    @Size(max = 20)
    private String city;

    @NotBlank
    @Size(max = 20)
    private String district;

    @NotBlank
    @Size(max = 100)
    private String detailAddress;

    /** 是否设为默认 */
    private Boolean setDefault;
}
```

- [ ] **Step 33.4: AddressVO**

```java
package com.bookstore.domain.vo.address;

import lombok.Data;

@Data
public class AddressVO {

    private Long id;
    private String receiver;
    private String phone;
    private String province;
    private String city;
    private String district;
    private String detailAddress;
    private Boolean isDefault;

    /** 拼装好的完整地址,前端展示用 */
    private String fullAddress;
}
```

- [ ] **Step 33.5: 编译**

```bash
mvn -pl bookstore-mapper -am compile
```

- [ ] **Step 33.6: Commit**

```bash
git add .
git commit -m "feat(address): entity + mapper + DTO/VO"
```

---

### Task 34: AddressService.list + create

**Files:**
- Create: `bookstore-service/src/main/java/com/bookstore/service/address/AddressService.java`
- Create: `bookstore-service/src/main/java/com/bookstore/service/address/impl/AddressServiceImpl.java`
- Create: `bookstore-service/src/test/java/com/bookstore/service/address/AddressServiceListCreateTest.java`

- [ ] **Step 34.1: 接口**

```java
package com.bookstore.service.address;

import com.bookstore.domain.dto.address.AddressFormDTO;
import com.bookstore.domain.vo.address.AddressVO;

import java.util.List;

public interface AddressService {

    List<AddressVO> list(Long userId);

    AddressVO create(Long userId, AddressFormDTO dto);

    AddressVO update(Long userId, Long id, AddressFormDTO dto);

    void delete(Long userId, Long id);

    void setDefault(Long userId, Long id);
}
```

- [ ] **Step 34.2: AddressServiceImpl 骨架**

```java
package com.bookstore.service.address.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bookstore.common.exception.BusinessException;
import com.bookstore.common.result.ResultCode;
import com.bookstore.domain.dto.address.AddressFormDTO;
import com.bookstore.domain.po.Address;
import com.bookstore.domain.vo.address.AddressVO;
import com.bookstore.mapper.AddressMapper;
import com.bookstore.service.address.AddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AddressServiceImpl implements AddressService {

    private final AddressMapper addressMapper;

    @Override
    public List<AddressVO> list(Long userId) {
        List<Address> rows = addressMapper.selectList(
            new LambdaQueryWrapper<Address>()
                .eq(Address::getUserId, userId)
                .orderByDesc(Address::getIsDefault)
                .orderByDesc(Address::getId)
        );
        return rows.stream().map(this::toVO).toList();
    }

    @Override
    @Transactional
    public AddressVO create(Long userId, AddressFormDTO dto) {
        Address a = new Address();
        a.setUserId(userId);
        a.setReceiver(dto.getReceiver());
        a.setPhone(dto.getPhone());
        a.setProvince(dto.getProvince());
        a.setCity(dto.getCity());
        a.setDistrict(dto.getDistrict());
        a.setDetailAddress(dto.getDetailAddress());

        boolean wantDefault = Boolean.TRUE.equals(dto.getSetDefault());
        // 用户第一次新增,默认就是默认地址
        Long count = addressMapper.selectCount(
            new LambdaQueryWrapper<Address>().eq(Address::getUserId, userId)
        );
        a.setIsDefault((wantDefault || count == 0) ? 1 : 0);

        addressMapper.insert(a);

        // 如果设为默认,把同用户其他地址清掉默认标
        if (a.getIsDefault() == 1) {
            addressMapper.unsetOtherDefaults(userId, a.getId());
        }
        return toVO(a);
    }

    @Override
    public AddressVO update(Long userId, Long id, AddressFormDTO dto) {
        throw new UnsupportedOperationException("update 在 Task 35 实现");
    }

    @Override
    public void delete(Long userId, Long id) {
        throw new UnsupportedOperationException("delete 在 Task 35 实现");
    }

    @Override
    public void setDefault(Long userId, Long id) {
        throw new UnsupportedOperationException("setDefault 在 Task 36 实现");
    }

    Address requireOwn(Long userId, Long id) {
        Address a = addressMapper.selectById(id);
        if (a == null || !a.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        return a;
    }

    private AddressVO toVO(Address a) {
        AddressVO v = new AddressVO();
        v.setId(a.getId());
        v.setReceiver(a.getReceiver());
        v.setPhone(a.getPhone());
        v.setProvince(a.getProvince());
        v.setCity(a.getCity());
        v.setDistrict(a.getDistrict());
        v.setDetailAddress(a.getDetailAddress());
        v.setIsDefault(a.getIsDefault() != null && a.getIsDefault() == 1);
        v.setFullAddress(a.getProvince() + a.getCity() + a.getDistrict() + a.getDetailAddress());
        return v;
    }
}
```

- [ ] **Step 34.3: 测试 list + create**

```java
package com.bookstore.service.address;

import com.bookstore.domain.dto.address.AddressFormDTO;
import com.bookstore.domain.po.Address;
import com.bookstore.domain.vo.address.AddressVO;
import com.bookstore.mapper.AddressMapper;
import com.bookstore.service.address.impl.AddressServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AddressServiceListCreateTest {

    @Mock AddressMapper addressMapper;
    @InjectMocks AddressServiceImpl addressService;

    private AddressFormDTO sampleForm() {
        AddressFormDTO f = new AddressFormDTO();
        f.setReceiver("张三"); f.setPhone("13800000001");
        f.setProvince("浙江"); f.setCity("杭州"); f.setDistrict("西湖区");
        f.setDetailAddress("文一西路 100 号");
        return f;
    }

    @Test
    void create_first_address_is_default() {
        when(addressMapper.selectCount(any())).thenReturn(0L);
        when(addressMapper.insert(any(Address.class))).thenAnswer(inv -> {
            inv.<Address>getArgument(0).setId(101L);
            return 1;
        });

        AddressFormDTO f = sampleForm();
        f.setSetDefault(false);  // 即使没要求,首张也要是默认

        AddressVO vo = addressService.create(1L, f);

        ArgumentCaptor<Address> captor = ArgumentCaptor.forClass(Address.class);
        verify(addressMapper).insert(captor.capture());
        assertThat(captor.getValue().getIsDefault()).isEqualTo(1);
        assertThat(vo.getIsDefault()).isTrue();
        verify(addressMapper).unsetOtherDefaults(eq(1L), eq(101L));
    }

    @Test
    void create_second_non_default_keeps_existing_default() {
        when(addressMapper.selectCount(any())).thenReturn(1L);
        when(addressMapper.insert(any(Address.class))).thenAnswer(inv -> {
            inv.<Address>getArgument(0).setId(202L);
            return 1;
        });

        AddressFormDTO f = sampleForm();
        f.setSetDefault(false);

        AddressVO vo = addressService.create(1L, f);

        assertThat(vo.getIsDefault()).isFalse();
        verify(addressMapper, never()).unsetOtherDefaults(anyLong(), anyLong());
    }

    @Test
    void list_orders_default_first() {
        Address a1 = new Address();
        a1.setId(1L); a1.setUserId(1L); a1.setIsDefault(0);
        a1.setReceiver("张三"); a1.setPhone("13800000001");
        a1.setProvince("浙江"); a1.setCity("杭州"); a1.setDistrict("西湖区"); a1.setDetailAddress("...");
        Address a2 = new Address();
        a2.setId(2L); a2.setUserId(1L); a2.setIsDefault(1);
        a2.setReceiver("李四"); a2.setPhone("13800000002");
        a2.setProvince("浙江"); a2.setCity("杭州"); a2.setDistrict("拱墅区"); a2.setDetailAddress("...");
        when(addressMapper.selectList(any())).thenReturn(List.of(a2, a1));  // mapper 已 order by

        List<AddressVO> list = addressService.list(1L);
        assertThat(list).hasSize(2);
        assertThat(list.get(0).getIsDefault()).isTrue();
    }
}
```

- [ ] **Step 34.4: 跑测试**

```bash
mvn -pl bookstore-service -am test -Dtest=AddressServiceListCreateTest
```

- [ ] **Step 34.5: Commit**

```bash
git add .
git commit -m "feat(address): list + create with first-address-default rule"
```

---

### Task 35: AddressService.update + delete

**Files:**
- Modify: `bookstore-service/src/main/java/com/bookstore/service/address/impl/AddressServiceImpl.java` (实现 update + delete)
- Create: `bookstore-service/src/test/java/com/bookstore/service/address/AddressServiceUpdateDeleteTest.java`

`update` 重要点:`requireOwn` 防越权(B 用户改 A 用户地址)。
`delete` 重要点:用 MyBatis-Plus 的逻辑删除即可,但要拦"删除唯一默认地址"的情况(没有了默认怎么办?后端策略:**自动把最近一条非默认升为默认**)。

- [ ] **Step 35.1: 写失败的测试**

```java
package com.bookstore.service.address;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bookstore.common.exception.BusinessException;
import com.bookstore.common.result.ResultCode;
import com.bookstore.domain.dto.address.AddressFormDTO;
import com.bookstore.domain.po.Address;
import com.bookstore.domain.vo.address.AddressVO;
import com.bookstore.mapper.AddressMapper;
import com.bookstore.service.address.impl.AddressServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AddressServiceUpdateDeleteTest {

    @Mock AddressMapper addressMapper;
    @InjectMocks AddressServiceImpl addressService;

    private Address own(Long userId, Long id, int isDefault) {
        Address a = new Address();
        a.setId(id); a.setUserId(userId); a.setIsDefault(isDefault);
        a.setReceiver("张三"); a.setPhone("13800000001");
        a.setProvince("浙江"); a.setCity("杭州"); a.setDistrict("西湖区"); a.setDetailAddress("...");
        return a;
    }

    @Test
    void update_changes_fields() {
        when(addressMapper.selectById(10L)).thenReturn(own(1L, 10L, 0));

        AddressFormDTO dto = new AddressFormDTO();
        dto.setReceiver("李四"); dto.setPhone("13900000000");
        dto.setProvince("浙江"); dto.setCity("杭州"); dto.setDistrict("拱墅区");
        dto.setDetailAddress("莫干山路 100 号");

        AddressVO vo = addressService.update(1L, 10L, dto);
        assertThat(vo.getReceiver()).isEqualTo("李四");
        verify(addressMapper).updateById(any(Address.class));
    }

    @Test
    void update_rejects_other_user_address() {
        when(addressMapper.selectById(10L)).thenReturn(own(2L, 10L, 0));  // 属于用户 2

        AddressFormDTO dto = new AddressFormDTO();
        dto.setReceiver("X"); dto.setPhone("13800000001");
        dto.setProvince("X"); dto.setCity("X"); dto.setDistrict("X");
        dto.setDetailAddress("X");

        assertThatThrownBy(() -> addressService.update(1L, 10L, dto))
            .isInstanceOf(BusinessException.class)
            .extracting("code").isEqualTo(ResultCode.FORBIDDEN.getCode());
    }

    @Test
    void delete_default_promotes_next() {
        // 当前要删的就是默认
        when(addressMapper.selectById(10L)).thenReturn(own(1L, 10L, 1));
        // 用户还有 11 / 12 两条非默认
        Address other = own(1L, 12L, 0);
        when(addressMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(List.of(other));

        addressService.delete(1L, 10L);

        verify(addressMapper).deleteById(10L);
        // 最近一条被升为默认
        verify(addressMapper).updateById(argThat(a ->
            a.getId().equals(12L) && a.getIsDefault() != null && a.getIsDefault() == 1));
    }

    @Test
    void delete_non_default_does_not_touch_others() {
        when(addressMapper.selectById(11L)).thenReturn(own(1L, 11L, 0));
        addressService.delete(1L, 11L);
        verify(addressMapper).deleteById(11L);
        verify(addressMapper, never()).updateById(any(Address.class));
    }
}
```

- [ ] **Step 35.2: 实现 update**

替换 `AddressServiceImpl.update`:

```java
@Override
@Transactional
public AddressVO update(Long userId, Long id, AddressFormDTO dto) {
    Address a = requireOwn(userId, id);

    Address patch = new Address();
    patch.setId(id);
    patch.setReceiver(dto.getReceiver());
    patch.setPhone(dto.getPhone());
    patch.setProvince(dto.getProvince());
    patch.setCity(dto.getCity());
    patch.setDistrict(dto.getDistrict());
    patch.setDetailAddress(dto.getDetailAddress());
    addressMapper.updateById(patch);

    a.setReceiver(dto.getReceiver());
    a.setPhone(dto.getPhone());
    a.setProvince(dto.getProvince());
    a.setCity(dto.getCity());
    a.setDistrict(dto.getDistrict());
    a.setDetailAddress(dto.getDetailAddress());
    return toVO(a);
}
```

- [ ] **Step 35.3: 实现 delete**

替换 `AddressServiceImpl.delete`:

```java
@Override
@Transactional
public void delete(Long userId, Long id) {
    Address a = requireOwn(userId, id);
    addressMapper.deleteById(id);  // 走 @TableLogic 软删

    // 删的是默认 → 自动提升另一条
    if (a.getIsDefault() != null && a.getIsDefault() == 1) {
        var others = addressMapper.selectList(
            new LambdaQueryWrapper<Address>()
                .eq(Address::getUserId, userId)
                .ne(Address::getId, id)
                .orderByDesc(Address::getId)
                .last("LIMIT 1")
        );
        if (!others.isEmpty()) {
            Address promote = new Address();
            promote.setId(others.get(0).getId());
            promote.setIsDefault(1);
            addressMapper.updateById(promote);
        }
    }
}
```

- [ ] **Step 35.4: 跑测试**

```bash
mvn -pl bookstore-service -am test -Dtest=AddressServiceUpdateDeleteTest
```

期望:4 个 PASS。

- [ ] **Step 35.5: Commit**

```bash
git add .
git commit -m "feat(address): update + delete with auto-promote default"
```

---

### Task 36: AddressService.setDefault + AddressController

**Files:**
- Modify: `bookstore-service/src/main/java/com/bookstore/service/address/impl/AddressServiceImpl.java` (实现 setDefault)
- Create: `bookstore-web-api/src/main/java/com/bookstore/api/address/AddressController.java`
- Create: `bookstore-web-api/src/test/java/com/bookstore/api/address/AddressControllerSliceTest.java`

setDefault 是简单的两步事务:目标置 1 → 其余置 0。`unsetOtherDefaults` 已经在 mapper 写好。

- [ ] **Step 36.1: 实现 setDefault**

替换 `AddressServiceImpl.setDefault`:

```java
@Override
@Transactional
public void setDefault(Long userId, Long id) {
    requireOwn(userId, id);
    Address patch = new Address();
    patch.setId(id);
    patch.setIsDefault(1);
    addressMapper.updateById(patch);
    addressMapper.unsetOtherDefaults(userId, id);
}
```

- [ ] **Step 36.2: AddressController**

```java
package com.bookstore.api.address;

import com.bookstore.common.annotation.LoginRequired;
import com.bookstore.common.context.UserContext;
import com.bookstore.common.result.Result;
import com.bookstore.domain.dto.address.AddressFormDTO;
import com.bookstore.domain.vo.address.AddressVO;
import com.bookstore.service.address.AddressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/address")
@RequiredArgsConstructor
@LoginRequired
public class AddressController {

    private final AddressService addressService;

    @GetMapping
    public Result<List<AddressVO>> list() {
        return Result.ok(addressService.list(UserContext.requireUserId()));
    }

    @PostMapping
    public Result<AddressVO> create(@Valid @RequestBody AddressFormDTO dto) {
        return Result.ok(addressService.create(UserContext.requireUserId(), dto));
    }

    @PutMapping("/{id}")
    public Result<AddressVO> update(@PathVariable Long id,
                                    @Valid @RequestBody AddressFormDTO dto) {
        return Result.ok(addressService.update(UserContext.requireUserId(), id, dto));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        addressService.delete(UserContext.requireUserId(), id);
        return Result.ok();
    }

    @PutMapping("/{id}/default")
    public Result<Void> setDefault(@PathVariable Long id) {
        addressService.setDefault(UserContext.requireUserId(), id);
        return Result.ok();
    }
}
```

- [ ] **Step 36.3: 切片测试**

```java
package com.bookstore.api.address;

import com.bookstore.common.context.UserContext;
import com.bookstore.domain.dto.address.AddressFormDTO;
import com.bookstore.domain.vo.address.AddressVO;
import com.bookstore.service.address.AddressService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AddressController.class)
@Import(com.bookstore.common.exception.GlobalExceptionHandler.class)
class AddressControllerSliceTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;
    @MockBean AddressService addressService;

    @BeforeEach
    void setUp() { UserContext.setUserId(1L); UserContext.setRole("USER"); }
    @AfterEach
    void tearDown() { UserContext.clear(); }

    private AddressVO sampleVO() {
        AddressVO v = new AddressVO();
        v.setId(101L); v.setReceiver("张三"); v.setPhone("13800000001");
        v.setProvince("浙江"); v.setCity("杭州"); v.setDistrict("西湖区");
        v.setDetailAddress("文一西路 100"); v.setIsDefault(true);
        v.setFullAddress("浙江杭州西湖区文一西路 100");
        return v;
    }

    @Test
    void list_returns_array() throws Exception {
        when(addressService.list(1L)).thenReturn(List.of(sampleVO()));
        mvc.perform(get("/api/address"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].id").value(101));
    }

    @Test
    void create_validates_phone_format() throws Exception {
        AddressFormDTO dto = new AddressFormDTO();
        dto.setReceiver("张三"); dto.setPhone("12345");
        dto.setProvince("浙"); dto.setCity("杭"); dto.setDistrict("西");
        dto.setDetailAddress("...");

        mvc.perform(post("/api/address")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(dto)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void create_succeeds_with_valid_form() throws Exception {
        when(addressService.create(eq(1L), any(AddressFormDTO.class))).thenReturn(sampleVO());

        AddressFormDTO dto = new AddressFormDTO();
        dto.setReceiver("张三"); dto.setPhone("13800000001");
        dto.setProvince("浙江"); dto.setCity("杭州"); dto.setDistrict("西湖区");
        dto.setDetailAddress("文一西路 100");
        dto.setSetDefault(true);

        mvc.perform(post("/api/address")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(dto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.isDefault").value(true));
    }

    @Test
    void setDefault_returns_ok() throws Exception {
        mvc.perform(put("/api/address/101/default"))
            .andExpect(status().isOk());
        verify(addressService).setDefault(1L, 101L);
    }

    @Test
    void delete_returns_ok() throws Exception {
        mvc.perform(delete("/api/address/101"))
            .andExpect(status().isOk());
        verify(addressService).delete(1L, 101L);
    }
}
```

- [ ] **Step 36.4: 跑测试**

```bash
mvn -pl bookstore-web-api -am test -Dtest=AddressControllerSliceTest
```

期望:5 个 PASS。

- [ ] **Step 36.5: Commit**

```bash
git add .
git commit -m "feat(address): setDefault + AddressController with slice tests"
```

---

## Phase 10: 集成验收 + 打 tag

**目标:** Phase 7-9 都是 mock 隔离的单元/切片测试,这一阶段补一个真链路测试 — Spring Boot + Testcontainers 起真 MySQL,跑一遍"注册 → 登录 → 拿 token 看 profile → 加地址 → 设默认 → 登出"。同时输出 Postman collection 给前端同事和答辩用。

---

### Task 37: Postman Collection 导出

**Files:**
- Create: `docs/postman/bookstore-auth.postman_collection.json`

> 这一步不是必须用 Postman 写,直接拿这份 JSON 导入 Postman 或 Apifox 都能用。如果不想写,跳到 Step 37.3 用 Bruno 或 README 表格替代,但**必须有一份给前端**。

- [ ] **Step 37.1: 创建目录**

```bash
mkdir -p docs/postman
```

- [ ] **Step 37.2: 写 collection**

```json
{
  "info": {
    "name": "Bookstore - Auth & User & Address",
    "_postman_id": "bookstore-plan1-v0.1",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "variable": [
    { "key": "baseUrl", "value": "http://localhost:8080" },
    { "key": "accessToken", "value": "" },
    { "key": "refreshToken", "value": "" }
  ],
  "item": [
    {
      "name": "Auth",
      "item": [
        {
          "name": "Register",
          "request": {
            "method": "POST",
            "header": [{ "key": "Content-Type", "value": "application/json" }],
            "url": "{{baseUrl}}/api/auth/register",
            "body": {
              "mode": "raw",
              "raw": "{\n  \"username\": \"alice\",\n  \"password\": \"hello123\",\n  \"phone\": \"13800000001\"\n}"
            }
          },
          "event": [{
            "listen": "test",
            "script": { "exec": [
              "var json = pm.response.json();",
              "pm.collectionVariables.set('accessToken', json.data.accessToken);",
              "pm.collectionVariables.set('refreshToken', json.data.refreshToken);"
            ] }
          }]
        },
        {
          "name": "Login",
          "request": {
            "method": "POST",
            "header": [{ "key": "Content-Type", "value": "application/json" }],
            "url": "{{baseUrl}}/api/auth/login",
            "body": {
              "mode": "raw",
              "raw": "{\n  \"account\": \"alice\",\n  \"password\": \"hello123\"\n}"
            }
          },
          "event": [{
            "listen": "test",
            "script": { "exec": [
              "var json = pm.response.json();",
              "pm.collectionVariables.set('accessToken', json.data.accessToken);",
              "pm.collectionVariables.set('refreshToken', json.data.refreshToken);"
            ] }
          }]
        },
        {
          "name": "Refresh",
          "request": {
            "method": "POST",
            "header": [{ "key": "Content-Type", "value": "application/json" }],
            "url": "{{baseUrl}}/api/auth/refresh",
            "body": {
              "mode": "raw",
              "raw": "{\n  \"refreshToken\": \"{{refreshToken}}\"\n}"
            }
          }
        },
        {
          "name": "Logout",
          "request": {
            "method": "POST",
            "header": [{ "key": "Authorization", "value": "Bearer {{accessToken}}" }],
            "url": "{{baseUrl}}/api/auth/logout"
          }
        }
      ]
    },
    {
      "name": "User",
      "item": [
        {
          "name": "GET /api/user/me",
          "request": {
            "method": "GET",
            "header": [{ "key": "Authorization", "value": "Bearer {{accessToken}}" }],
            "url": "{{baseUrl}}/api/user/me"
          }
        },
        {
          "name": "PUT /api/user/me",
          "request": {
            "method": "PUT",
            "header": [
              { "key": "Authorization", "value": "Bearer {{accessToken}}" },
              { "key": "Content-Type", "value": "application/json" }
            ],
            "url": "{{baseUrl}}/api/user/me",
            "body": { "mode": "raw", "raw": "{\n  \"nickname\": \"小明\",\n  \"gender\": 1\n}" }
          }
        },
        {
          "name": "PUT /api/user/me/password",
          "request": {
            "method": "PUT",
            "header": [
              { "key": "Authorization", "value": "Bearer {{accessToken}}" },
              { "key": "Content-Type", "value": "application/json" }
            ],
            "url": "{{baseUrl}}/api/user/me/password",
            "body": { "mode": "raw", "raw": "{\n  \"oldPassword\": \"hello123\",\n  \"newPassword\": \"hello456\"\n}" }
          }
        },
        {
          "name": "PUT /api/user/me/avatar",
          "request": {
            "method": "PUT",
            "header": [
              { "key": "Authorization", "value": "Bearer {{accessToken}}" },
              { "key": "Content-Type", "value": "application/json" }
            ],
            "url": "{{baseUrl}}/api/user/me/avatar",
            "body": { "mode": "raw", "raw": "{\n  \"avatarKey\": \"avatars/1.jpg\"\n}" }
          }
        }
      ]
    },
    {
      "name": "Address",
      "item": [
        {
          "name": "GET /api/address",
          "request": {
            "method": "GET",
            "header": [{ "key": "Authorization", "value": "Bearer {{accessToken}}" }],
            "url": "{{baseUrl}}/api/address"
          }
        },
        {
          "name": "POST /api/address",
          "request": {
            "method": "POST",
            "header": [
              { "key": "Authorization", "value": "Bearer {{accessToken}}" },
              { "key": "Content-Type", "value": "application/json" }
            ],
            "url": "{{baseUrl}}/api/address",
            "body": { "mode": "raw", "raw": "{\n  \"receiver\": \"张三\",\n  \"phone\": \"13800000001\",\n  \"province\": \"浙江\",\n  \"city\": \"杭州\",\n  \"district\": \"西湖区\",\n  \"detailAddress\": \"文一西路 100 号\",\n  \"setDefault\": true\n}" }
          }
        },
        {
          "name": "PUT /api/address/{id}/default",
          "request": {
            "method": "PUT",
            "header": [{ "key": "Authorization", "value": "Bearer {{accessToken}}" }],
            "url": "{{baseUrl}}/api/address/1/default"
          }
        },
        {
          "name": "DELETE /api/address/{id}",
          "request": {
            "method": "DELETE",
            "header": [{ "key": "Authorization", "value": "Bearer {{accessToken}}" }],
            "url": "{{baseUrl}}/api/address/1"
          }
        }
      ]
    }
  ]
}
```

- [ ] **Step 37.3: 在 README 里贴一张 13 个端点的速查表**

打开/创建项目根 `README.md`,在 `## API 列表 (Plan 1)` 章节下追加:

```markdown
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

完整 Postman collection 在 `docs/postman/bookstore-auth.postman_collection.json`。
```

- [ ] **Step 37.4: Commit**

```bash
git add docs/postman README.md
git commit -m "docs: Postman collection + endpoint cheatsheet for Plan 1"
```

---

### Task 38: Testcontainers 集成测试基础设施

**Files:**
- Modify: parent `pom.xml` 在 `<dependencyManagement>` 加 Testcontainers BOM
- Create: `bookstore-web-api/src/test/java/com/bookstore/api/it/IntegrationTestBase.java`
- Modify: `bookstore-web-api/pom.xml` 加 testcontainers + spring-boot-starter-test

实测要起真 MySQL(Flyway 跑迁移)+ Redis(JWT 黑名单)。Spring Boot 3.1+ 提供 `@ServiceConnection` 自动注入连接信息,不用手写 `@DynamicPropertySource`。

- [ ] **Step 38.1: 在 parent pom `<dependencyManagement>` 加 Testcontainers BOM**

打开 Task 1 的 parent `pom.xml`,在 `<dependencyManagement><dependencies>` 内追加:

```xml
            <dependency>
                <groupId>org.testcontainers</groupId>
                <artifactId>testcontainers-bom</artifactId>
                <version>1.19.7</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
```

- [ ] **Step 38.2: bookstore-web-api/pom.xml 追加测试依赖**

```xml
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>junit-jupiter</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>mysql</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>com.redis</groupId>
            <artifactId>testcontainers-redis</artifactId>
            <version>2.2.2</version>
            <scope>test</scope>
        </dependency>
```

- [ ] **Step 38.3: IntegrationTestBase**

```java
package com.bookstore.api.it;

import com.redis.testcontainers.RedisContainer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("it")
@Testcontainers
public abstract class IntegrationTestBase {

    @Container
    @ServiceConnection
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>(DockerImageName.parse("mysql:8.0"))
        .withDatabaseName("bookstore")
        .withUsername("test")
        .withPassword("test");

    @Container
    @ServiceConnection(name = "redis")
    static final RedisContainer REDIS = new RedisContainer(DockerImageName.parse("redis:7-alpine"));
}
```

- [ ] **Step 38.4: src/test/resources/application-it.yml**

文件位置:`bookstore-web-api/src/test/resources/application-it.yml`

```yaml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
  jpa:
    hibernate:
      ddl-auto: none

bookstore:
  jwt:
    secret: integration-test-secret-must-be-long-enough-for-hs256-aaaaaaaa
    access-ttl: 2h
    refresh-ttl: 7d
  oss:
    public-prefix: https://test-oss.example.com/

logging:
  level:
    root: WARN
    com.bookstore: INFO
```

- [ ] **Step 38.5: 把 V1__init_schema.sql 复制到 web-api 的测试 resources(让 Flyway 在测试时也能看到迁移)**

在 `bookstore-web-api/pom.xml` 加一个 resources 复制配置(或更简单:把 schema 留在 `bookstore-mapper/src/main/resources/db/migration`,然后 web-api 通过依赖 jar 自动 classpath 拿到。**推荐第二种** — 不复制文件,用 jar 内 classpath)。

> **如果走第二种**:回头确认 Task 5 的 V1 SQL 文件路径是 `bookstore-mapper/src/main/resources/db/migration/V1__init_schema.sql`(不是 web-api),Flyway 默认会扫描所有 jar 的 `db/migration/`。

- [ ] **Step 38.6: 写一个最小冒烟测试,验证容器能起来**

```java
package com.bookstore.api.it;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class SmokeIT extends IntegrationTestBase {

    @Autowired TestRestTemplate restTemplate;

    @Test
    void actuator_health_returns_200() {
        ResponseEntity<String> resp = restTemplate.getForEntity("/actuator/health", String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).contains("\"status\":\"UP\"");
    }
}
```

- [ ] **Step 38.7: 跑冒烟测试(注意:必须有 Docker 在跑)**

```bash
mvn -pl bookstore-web-api -am test -Dtest=SmokeIT
```

期望:启动慢一点(首次拉镜像几十秒),最后 PASS。

> **常见问题**:
> 1. Docker 没启动 — 报 `Could not find a valid Docker environment`,启动 Docker Desktop。
> 2. `redis-testcontainers` 包冲突 — 用上面给的版本 `2.2.2`,与 1.19.x 兼容。
> 3. Flyway 报 schema 冲突 — 容器是干净的,不应有冲突;如有,清掉 `target` 重跑。

- [ ] **Step 38.8: Commit**

```bash
git add .
git commit -m "test(it): Testcontainers MySQL+Redis IntegrationTestBase + smoke IT"
```

---

### Task 39: 端到端注册/登录/资料/地址 IT

**Files:**
- Create: `bookstore-web-api/src/test/java/com/bookstore/api/it/AuthFlowIT.java`

这是 Plan 1 最后的"验收测试":一个 IT 把整条链路串起来,模拟前端完整调用一次。**这个测试通过 = Plan 1 验收完成**。

- [ ] **Step 39.1: 写 IT**

```java
package com.bookstore.api.it;

import com.bookstore.common.result.Result;
import com.bookstore.domain.dto.address.AddressFormDTO;
import com.bookstore.domain.dto.auth.LoginDTO;
import com.bookstore.domain.dto.auth.RegisterDTO;
import com.bookstore.domain.dto.user.UpdateProfileDTO;
import com.bookstore.domain.vo.address.AddressVO;
import com.bookstore.domain.vo.auth.TokenVO;
import com.bookstore.domain.vo.user.UserProfileVO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuthFlowIT extends IntegrationTestBase {

    @Autowired TestRestTemplate restTemplate;
    @Autowired ObjectMapper om;

    static String accessToken;
    static String refreshToken;
    static Long addressId;

    private HttpHeaders bearer() {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.setBearerAuth(accessToken);
        return h;
    }

    private HttpHeaders json() {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        return h;
    }

    private <T> T extractData(String body, Class<T> clazz) throws Exception {
        Result<?> r = om.readValue(body, Result.class);
        return om.convertValue(r.getData(), clazz);
    }

    @Test @Order(1)
    void register_returns_token_and_user() throws Exception {
        RegisterDTO dto = new RegisterDTO();
        dto.setUsername("alice"); dto.setPassword("hello123"); dto.setPhone("13800000001");

        ResponseEntity<String> resp = restTemplate.exchange(
            "/api/auth/register", HttpMethod.POST,
            new HttpEntity<>(om.writeValueAsString(dto), json()), String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        TokenVO vo = extractData(resp.getBody(), TokenVO.class);
        assertThat(vo.getAccessToken()).isNotBlank();
        assertThat(vo.getUser().getUsername()).isEqualTo("alice");
        accessToken = vo.getAccessToken();
        refreshToken = vo.getRefreshToken();
    }

    @Test @Order(2)
    void duplicate_registration_returns_business_error() throws Exception {
        RegisterDTO dto = new RegisterDTO();
        dto.setUsername("alice"); dto.setPassword("hello123"); dto.setPhone("13800000001");

        ResponseEntity<String> resp = restTemplate.exchange(
            "/api/auth/register", HttpMethod.POST,
            new HttpEntity<>(om.writeValueAsString(dto), json()), String.class);

        // GlobalExceptionHandler 把 BusinessException 映射到 200+code,或 4xx,看你设计;假定 4xx
        assertThat(resp.getStatusCode().is4xxClientError() || resp.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(resp.getBody()).contains("用户名已被注册");
    }

    @Test @Order(3)
    void me_returns_masked_phone() throws Exception {
        ResponseEntity<String> resp = restTemplate.exchange(
            "/api/user/me", HttpMethod.GET,
            new HttpEntity<>(bearer()), String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        UserProfileVO vo = extractData(resp.getBody(), UserProfileVO.class);
        assertThat(vo.getPhone()).matches("138\\*\\*\\*\\*0001");
        assertThat(vo.getRole()).isEqualTo("USER");
    }

    @Test @Order(4)
    void update_profile_changes_nickname() throws Exception {
        UpdateProfileDTO dto = new UpdateProfileDTO();
        dto.setNickname("小明");

        ResponseEntity<String> resp = restTemplate.exchange(
            "/api/user/me", HttpMethod.PUT,
            new HttpEntity<>(om.writeValueAsString(dto), bearer()), String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        UserProfileVO vo = extractData(resp.getBody(), UserProfileVO.class);
        assertThat(vo.getNickname()).isEqualTo("小明");
    }

    @Test @Order(5)
    void create_address_first_is_default() throws Exception {
        AddressFormDTO f = new AddressFormDTO();
        f.setReceiver("张三"); f.setPhone("13800000001");
        f.setProvince("浙江"); f.setCity("杭州"); f.setDistrict("西湖区");
        f.setDetailAddress("文一西路 100 号");

        ResponseEntity<String> resp = restTemplate.exchange(
            "/api/address", HttpMethod.POST,
            new HttpEntity<>(om.writeValueAsString(f), bearer()), String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        AddressVO vo = extractData(resp.getBody(), AddressVO.class);
        assertThat(vo.getIsDefault()).isTrue();  // 首张自动默认
        addressId = vo.getId();
    }

    @Test @Order(6)
    void list_address_includes_created() throws Exception {
        ResponseEntity<String> resp = restTemplate.exchange(
            "/api/address", HttpMethod.GET,
            new HttpEntity<>(bearer()), String.class);

        Result<?> r = om.readValue(resp.getBody(), Result.class);
        List<AddressVO> list = om.convertValue(r.getData(), new TypeReference<>() {});
        assertThat(list).extracting(AddressVO::getId).contains(addressId);
    }

    @Test @Order(7)
    void unauth_request_returns_401() {
        ResponseEntity<String> resp = restTemplate.getForEntity("/api/user/me", String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test @Order(8)
    void logout_then_token_blacklisted() throws Exception {
        // logout
        restTemplate.exchange("/api/auth/logout", HttpMethod.POST,
            new HttpEntity<>(bearer()), String.class);

        // 再用同一 token 调 /api/user/me 应该 401
        ResponseEntity<String> resp = restTemplate.exchange(
            "/api/user/me", HttpMethod.GET,
            new HttpEntity<>(bearer()), String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test @Order(9)
    void login_again_then_refresh_returns_new_pair() throws Exception {
        LoginDTO login = new LoginDTO();
        login.setAccount("alice"); login.setPassword("hello123");
        ResponseEntity<String> r1 = restTemplate.exchange(
            "/api/auth/login", HttpMethod.POST,
            new HttpEntity<>(om.writeValueAsString(login), json()), String.class);
        TokenVO vo1 = extractData(r1.getBody(), TokenVO.class);

        // refresh
        String body = "{\"refreshToken\":\"" + vo1.getRefreshToken() + "\"}";
        ResponseEntity<String> r2 = restTemplate.exchange(
            "/api/auth/refresh", HttpMethod.POST,
            new HttpEntity<>(body, json()), String.class);
        TokenVO vo2 = extractData(r2.getBody(), TokenVO.class);

        assertThat(vo2.getAccessToken()).isNotEqualTo(vo1.getAccessToken());
        assertThat(vo2.getRefreshToken()).isNotEqualTo(vo1.getRefreshToken());

        // 旧 refresh 再用应当 401
        ResponseEntity<String> r3 = restTemplate.exchange(
            "/api/auth/refresh", HttpMethod.POST,
            new HttpEntity<>(body, json()), String.class);
        assertThat(r3.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
```

- [ ] **Step 39.2: 跑这个 IT**

```bash
mvn -pl bookstore-web-api -am test -Dtest=AuthFlowIT
```

期望:9 个 PASS,顺序执行(因为有 `@Order` + 共享 static 状态 — 这不是最佳实践,但对 Plan 1 这种单链路验收够用)。

> **如果某个测试失败**:常见原因是 GlobalExceptionHandler 对 `BusinessException` 的映射约定。打开 Task 8 的 handler,确认:
> - `BusinessException` → HTTP 200,body 里 `code` 是业务码 (推荐)
> - 或 → HTTP 400/401,body 同样有 code
> 测试里两种都接受了。如果你选了完全不同的策略,改测试的断言。

- [ ] **Step 39.3: Commit**

```bash
git add .
git commit -m "test(it): full register-login-profile-address flow"
```

---

### Task 40: 全量回归 + tag v0.1-auth-ready

**Files:**
- Modify: `README.md`(可选,记一行 changelog)

- [ ] **Step 40.1: 跑全量测试,所有模块**

```bash
mvn clean verify
```

期望:全部 PASS。如果有失败,**不要打 tag**,先修。

- [ ] **Step 40.2: 校验 13 个端点都能起**

```bash
mvn -pl bookstore-web-api spring-boot:run
```

另开终端,挨个 curl 一遍 Postman 里列出的端点,确保:
- 注册返回 token
- 登录返回 token
- 带 token 调 `/api/user/me` 返回脱敏 phone
- 不带 token 调 `/api/user/me` 返回 401
- 加一个地址,首张自动 isDefault=true
- 删除默认地址,另一条自动升默认

把成功响应截图或文本输出贴到 `docs/screenshots/plan1-acceptance.md`(可选,但答辩展示用得上)。

- [ ] **Step 40.3: 在 README 顶部写一段"已完成功能"清单**

```markdown
## 已完成 (v0.1-auth-ready)

- 多模块 Maven 脚手架,Spring Boot 3.2.5 + Java 17
- Flyway V1 完整 schema(user/address/book/order/...,Plan 2-6 会扩展)
- 三层异常体系:`BusinessException` / `AuthException` / `GlobalExceptionHandler`
- JWT 双 token + Redis 黑名单 + 黑名单 jti+TTL
- 用户认证:注册 / 登录 / 刷新 / 登出
- 用户资料:GET/PUT /me, 改密, 换头像
- 收货地址:CRUD + 默认切换 + 删默认自动提升
- Testcontainers 集成测试(MySQL + Redis)
- 13 个端点 Postman collection,IT 9 测全过
```

- [ ] **Step 40.4: 打 tag**

```bash
git add README.md
git commit -m "docs: v0.1-auth-ready changelog"
git tag -a v0.1-auth-ready -m "Plan 1 done: scaffold + auth + user + address"
```

> **不要 push tag** — 这是本地里程碑;答辩前再看是否需要推到远程。

- [ ] **Step 40.5: 验证 tag**

```bash
git tag --list
git log --oneline -20
```

确认 `v0.1-auth-ready` 出现,最新 20 条 commit 涵盖 Plan 1 全部 40 个 task 的提交。

---

## Plan 1 收尾自查清单

完成后,应该具备:

- [ ] `mvn clean verify` 全绿
- [ ] `mvn spring-boot:run -pl bookstore-web-api` 能起,actuator/health 返回 UP
- [ ] 13 个端点用 Postman/curl 都能调通
- [ ] `git tag --list` 有 `v0.1-auth-ready`
- [ ] `docs/postman/bookstore-auth.postman_collection.json` 存在
- [ ] `docs/superpowers/specs/2026-05-01-bookstore-design.md` 没有被 Plan 1 修改(只修代码)
- [ ] 9 个 IT 全过(`AuthFlowIT` + `SmokeIT`)
- [ ] 单元/切片测试覆盖:`AuthService*Test`(3 个), `UserService*Test`(3 个), `AddressService*Test`(2 个), `*ControllerSliceTest`(3 个), `GlobalExceptionHandlerTest`, `RateLimitAspectTest`(可选)、`PasswordUtilTest`

---

## 后续衔接

Plan 1 完成后,**Plan 2** 进入商品 + 交易主链路:

- 商品(book)CRUD + 库存盘点
- 购物车(Redis hash)
- 订单创建(扣库存事务)
- 支付回调 / 订单状态机
- 退款(简化版)

Plan 2 会**复用** Plan 1 的:`UserContext` / `LoginInterceptor` / `JwtUtil` / `Result` / `BaseEntity` / Testcontainers 基础设施。所以 Plan 1 的质量直接影响 Plan 2 的速度 — **不要跳过任何一个测试**。

