# 更新日志

本文档记录每次推送到 GitHub 的主要改动，按时间倒序排列。

---

## 2026-06-24

### 新增

- **用户阅读画像系统**
  - 新增 `user_reading_profile` 表（V12 迁移脚本），缓存 LLM 分析出的用户阅读偏好。
  - 新增 `UserReadingProfile` 实体、Mapper、Service。
  - 综合用户收藏、购物车、订单、搜索历史生成原始数据，调用 LLM 输出画像分析。
  - 画像默认缓存 7 天；每 2 小时定时刷新过期画像，白天/夜间采用不同批次与间隔策略。
  - 新用户首次搜索时，Agent 返回的画像分析会实时写入缓存。

- **AI 智能搜索优化**
  - AI 搜索优先使用缓存画像；无缓存时降级为原始行为数据，并同步缓存分析结果。
  - 新增 Spring Cache：热销候选图书 `ai:search:candidates`、搜索结果 `ai:search:results:v2`。
  - Agent 返回结果扩展为包含推荐原因与画像分析文本。
  - 新增异步搜索接口：
    - `POST /api/book/ai-search/async` 提交任务
    - `GET /api/book/ai-search/async/{taskId}` 轮询结果

- **可观测性增强**
  - 新增 `logback-spring.xml` 日志配置。
  - `BookController` 各接口增加请求进入/返回日志；AI 搜索接口返回 `durationMs` 耗时。
  - `AgentLifecycleManager` 启动 Python Agent 时打印美化横幅，并设置 `PYTHONIOENCODING=utf-8`。

### 其他

- 根目录 `.gitignore` 增加 `logs/`，避免本地日志误提交。

---
