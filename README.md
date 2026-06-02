# 智能书店系统 — 前端 (Bookstore Frontend)

毕设项目前端页面 — 纯原生 HTML + JavaScript + CSS，无前端框架。

## 技术栈

- HTML5
- CSS3
- 原生 JavaScript (ES6+)
- Nginx 静态资源托管

## 页面列表

### 用户端

| 页面 | 文件 | 说明 |
|------|------|------|
| 首页 | `index.html` | 图书列表、分类浏览、搜索 |
| 图书详情 | `detail.html` | 图书信息、评价、加入购物车 |
| 购物车 | `cart.html` | 购物车管理、结算 |
| 订单 | `order.html` | 订单确认、收货地址选择 |
| 订单详情 | `order-detail.html` | 订单状态、物流信息 |
| 用户中心 | `user.html` | 个人信息、订单历史 |
| 收货地址 | `address.html` | 地址管理 |
| 收藏 | `favorite.html` | 收藏图书列表 |
| 促销活动 | `promotions.html` | 优惠券、秒杀活动 |
| AI 智能助手 | `ai-chat.html` | 万事通对话、AI 搜索、AI 推荐 |

### 管理端

| 页面 | 文件 | 说明 |
|------|------|------|
| 仪表盘 | `admin/index.html` | 数据统计、运营概览 |
| 图书管理 | `admin/books.html` | 图书增删改查 |
| 分类管理 | `admin/categories.html` | 图书分类管理 |
| 订单管理 | `admin/orders.html` | 订单处理、发货 |
| 优惠券 | `admin/coupons.html` | 优惠券创建与管理 |
| 秒杀活动 | `admin/seckill.html` | 秒杀活动配置 |
| 操作日志 | `admin/logs.html` | 系统操作记录 |

## 部署

### Nginx 配置

将本目录配置为 Nginx 的 root：

```nginx
server {
    listen       80;
    server_name  localhost;

    location / {
        root   html/bookstore;
        index  index.html;
    }

    # API 代理到后端 Gateway
    location /api/ {
        proxy_pass http://localhost:8080;
    }
}
```

### 启动

```bash
# Windows
nginx.exe

# 或指定配置
nginx.exe -c conf/nginx.conf
```

访问 `http://localhost`

## 后端对接

前端通过 `fetch` 调用后端 API，默认对接后端 Gateway (`http://localhost:8080`)。

认证方式：JWT Token
- 登录成功后后端返回 `accessToken`
- 前端存储在 `localStorage`
- 后续请求自动携带 `Authorization: Bearer <token>`

## 目录结构

```
bookstore/
├── index.html              # 首页
├── home.html               # 首页（别名）
├── detail.html             # 图书详情
├── cart.html               # 购物车
├── order.html              # 订单
├── order-detail.html       # 订单详情
├── user.html               # 用户中心
├── address.html            # 收货地址
├── favorite.html           # 收藏
├── promotions.html         # 促销活动
├── ai-chat.html            # AI 智能助手
├── css/
│   └── style.css           # 全局样式
└── admin/
    ├── index.html          # 管理仪表盘
    ├── books.html          # 图书管理
    ├── categories.html     # 分类管理
    ├── orders.html         # 订单管理
    ├── coupons.html        # 优惠券
    ├── seckill.html        # 秒杀活动
    ├── logs.html           # 操作日志
    └── common.js           # 管理端公共脚本
```

## 注意事项

- 本前端为纯静态页面，所有数据通过 AJAX 从后端 API 获取
- 管理端页面需要管理员权限，未登录或权限不足会自动跳转
- AI 聊天页面支持 Markdown 渲染和代码高亮
