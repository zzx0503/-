# 智能书店系统 (Bookstore)

Spring Boot 3.2.5 单体后端毕设项目，涵盖图书商城、秒杀、AI 智能搜索/推荐、消息队列等。

## 技术栈

Spring Boot 3.2.5 + MyBatis-Plus 3.5.5 + MySQL 8.0 + Redis + RabbitMQ + Redisson + JWT + 阿里云 OSS + 阿里百炼 DashScope AI + Knife4j

## 功能

- 用户注册/登录（JWT 双 token）、个人资料、钱包
- 图书分类浏览、搜索、购物车、下单（优惠券）、收藏
- **AI 智能搜索**：自然语言搜书，AI 提取关键词后数据库多维检索
- **AI 推荐**：基于用户画像的个性化推荐
- **AI 对话**：多轮对话，图书咨询
- 秒杀（Redis 预售库存 + Lua 原子扣减 + RabbitMQ 排队削峰）
- 优惠券（后台模板 → 用户领取 → 下单计算最优方案）
- 每日签到领金币
- 后台管理（仪表盘、图书/分类/订单管理、操作日志）

## 快速开始

```bash
cd bookstore
cp src/main/resources/application-template.yml src/main/resources/application.yml
# 编辑 application.yml 填入数据库/Redis/RabbitMQ/OSS/AI 配置
mvn clean compile
mvn spring-boot:run
```

应用默认 `http://localhost:8080`，API 文档 `http://localhost:8080/doc.html`。

## 外部依赖

MySQL 8.0 / Redis / RabbitMQ / 阿里云 OSS / 阿里百炼 DashScope

## 项目结构

```
bookstore/
├── src/main/java/com/bookstore
│   ├── controller/        用户端 + admin 控制器
│   ├── service/           业务逻辑 + AI 客户端
│   ├── mapper/            MyBatis-Plus Mapper
│   ├── domain/            PO / DTO / VO
│   ├── config/            业务配置 (OSS, AI, MQ)
│   ├── filter/interceptor  JWT 鉴权 / CORS / 登录拦截
│   └── utils/             JWT / 密码加密 / OSS URL
├── src/main/resources/db/migration/  Flyway V1-V11
└── src/test/              单元测试 + 集成测试
```
