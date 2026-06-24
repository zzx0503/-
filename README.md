# "悦读云"智能图书电商与推荐平台

>Spring Boot 3 + LangChain FastAPI 双引擎，涵盖图书商城、AI 智能体搜索和推荐对、话、秒杀、优惠券、签到等完整功能。

## 技术栈

| 类别 | 技术 |
|------|------|
| 后端框架 | Spring Boot 3.2.5 + Spring MVC |
| ORM | MyBatis-Plus 3.5.5 |
| 数据库 | MySQL 8.0 + Flyway 迁移 |
| 缓存 | Redis + Redisson 3.27.2（分布式锁/限流） |
| 消息队列 | RabbitMQ + Spring AMQP |
| 认证 | JWT 双 token + Redis 黑名单 |
| 对象存储 | 阿里云 OSS |
| AI Agent | FastAPI + LangChain ReAct + qwen-turbo |
| API 文档 | Knife4j |

## 项目结构

```
悦读云/
├── bookstore/                    # Spring Boot 单体后端
│   ├── src/main/java/com/bookstore/
│   │   ├── controller/           用户端 + admin 控制器
│   │   ├── service/              业务逻辑 + AI 客户端
│   │   ├── service/ai/           Agent HTTP 客户端 (AiAgentClient)
│   │   ├── config/               AgentLifecycleManager（自动启停）
│   │   ├── mapper/              MyBatis-Plus Mapper
│   │   └── domain/              PO / DTO / VO
│   └── src/main/resources/
│       └── application-template.yml
│
└── ai-agent/                     # Python AI 智能体
    ├── app/
    │   ├── agent/                ReAct 核心 (core/prompts/tools/memory)
    │   ├── api/                  FastAPI 路由 (/search /recommend /chat)
    │   ├── client/               回调 bookstore 的工具客户端
    │   └── models/               Pydantic Schema
    ├── .env.example              环境变量模板
    └── requirements.txt
```

## 功能模块

### AI 智能体（核心亮点）
- **智能搜索**：自然语言输入 → Agent ReAct 推理 → JSON 返回推荐图书 ID + 逐条理由
- **快速搜索**：短词直查（书名/作者），毫秒级返回，0 次 AI 调用
- **个性化推荐**：结合用户画像（收藏/搜索历史）做端到端推理推荐
- **智能对话**：多轮对话，支持图书咨询、购物建议，Redis 会话记忆
- **工具链**：search_books / get_book_detail / get_user_profile / add_to_cart / web_search / fetch_book_review
- 模型 qwen-turbo，3 轮迭代上限，耗时 < 10s

### 其他模块
- 用户注册/登录（JWT 双 token）、个人资料、钱包
- 图书分类浏览、购物车、下单（优惠券）、收藏
- 秒杀（Redis 预售库存 + Lua 原子扣减 + RabbitMQ 排队削峰）
- 优惠券（后台模板 → 用户领取 → 下单计算最优方案）
- 每日签到领金币
- 后台管理（仪表盘、图书/分类/订单管理、操作日志）

## 快速开始

### 1. 启动依赖服务
MySQL 8.0 / Redis / RabbitMQ 需提前运行。

### 2. 配置
```bash
# Bookstore
cd bookstore
cp src/main/resources/application-template.yml src/main/resources/application.yml
# 编辑 application.yml 填入数据库/Redis/RabbitMQ/OSS/AI 配置

# AI Agent
cd ai-agent
cp .env.example .env
# 编辑 .env 填入 DashScope API Key
pip install -r requirements.txt
```

### 3. 启动
```bash
# 方式一：Bookstore 自动拉起 Agent（application.yml 中 auto-start: true）
cd bookstore
mvn spring-boot:run

# 方式二：分别启动
# 终端 1
cd ai-agent && venv\Scripts\python.exe -m uvicorn app.main:app --host 0.0.0.0 --port 8000
# 终端 2
cd bookstore && mvn spring-boot:run
```

> 应用 `http://localhost:8080` | API 文档 `http://localhost:8080/doc.html` | Agent `http://localhost:8000/health`

## 架构

```
浏览器 → Bookstore (Java:8080)
              │
              ├── POST /agent/search ────→ Agent (Python:8000)
              │   query + 30候选 + 用户画像     │ ReAct 推理
              │   ←── JSON {ids, reasons} ──    │ 工具调用
              │
              └── /internal/agent-tools/* ←── Agent 回调（查书/查用户）
```
