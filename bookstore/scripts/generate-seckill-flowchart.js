const pptxgen = require("pptxgenjs");

let pres = new pptxgen();
pres.layout = 'LAYOUT_16x9';
pres.author = 'Bookstore Team';
pres.title = '秒杀系统流程图';

// 配色方案：Teal Trust
const C = {
  primary: "028090",      // 主色（深蓝绿）
  secondary: "00A896",    // 次色（海泡绿）
  accent: "02C39A",       // 强调色（薄荷绿）
  light: "F8FAFC",        // 浅色背景
  white: "FFFFFF",
  dark: "1E293B",         // 深色文字
  body: "475569",         // 正文颜色
  muted: "94A3B8",        // 弱化颜色
  success: "10B981",      // 成功色
  warning: "F59E0B",      // 警告色
  error: "EF4444",        // 错误色
  cardBg: "FFFFFF",       // 卡片背景
  border: "E2E8F0"        // 边框色
};

// 辅助函数：添加侧边栏
function addSideBar(slide) {
  slide.addShape(pres.shapes.RECTANGLE, {
    x: 0, y: 0, w: 0.12, h: 5.625,
    fill: { color: C.secondary }
  });
}

// 辅助函数：添加标题
function addTitle(slide, text) {
  slide.addText(text, {
    x: 0.5, y: 0.3, w: 9, h: 0.7,
    fontSize: 32, bold: true, color: C.primary, fontFace: "Arial", margin: 0
  });
}

// 辅助函数：添加流程框
function addFlowBox(slide, text, x, y, w, h, bgColor, textColor, fontSize = 12) {
  slide.addShape(pres.shapes.ROUNDED_RECTANGLE, {
    x, y, w, h,
    fill: { color: bgColor || C.cardBg },
    line: { color: C.border, width: 1 },
    rectRadius: 0.1,
    shadow: { type: "outer", color: "000000", blur: 4, offset: 1, angle: 135, opacity: 0.1 }
  });
  slide.addText(text, {
    x, y, w, h,
    fontSize, bold: fontSize >= 12, color: textColor || C.body,
    fontFace: "Arial", margin: 0, valign: "middle", align: "center"
  });
}

// 辅助函数：添加箭头（垂直向下）
function addArrowDown(slide, x, y, length = 0.3) {
  slide.addShape(pres.shapes.LINE, {
    x, y, w: 0, h: length,
    line: { color: C.secondary, width: 2 }
  });
  // 箭头头部
  slide.addShape(pres.shapes.TRIANGLE, {
    x: x - 0.08, y: y + length - 0.05, w: 0.16, h: 0.15,
    fill: { color: C.secondary },
    line: { color: C.secondary, width: 0 }
  });
}

// 辅助函数：添加箭头（水平向右）
function addArrowRight(slide, x, y, length = 0.3) {
  slide.addShape(pres.shapes.LINE, {
    x, y, w: length, h: 0,
    line: { color: C.secondary, width: 2 }
  });
  // 箭头头部
  slide.addShape(pres.shapes.TRIANGLE, {
    x: x + length - 0.05, y: y - 0.08, w: 0.15, h: 0.16,
    fill: { color: C.secondary },
    line: { color: C.secondary, width: 0 }
  });
}

// 辅助函数：添加分支标签
function addBranchLabel(slide, text, x, y, color) {
  slide.addText(text, {
    x, y, w: 1.5, h: 0.3,
    fontSize: 10, color: color || C.muted,
    fontFace: "Arial", margin: 0
  });
}

// ========== Slide 1: 秒杀整体流程 ==========
let s1 = pres.addSlide();
s1.background = { color: C.light };
addSideBar(s1);
addTitle(s1, '秒杀系统 · 完整流程');

const startX = 0.8;
const boxW = 2.2;
const boxH = 0.6;
const arrowLen = 0.4;

// 第一行：用户请求 → Redis Lua 脚本
addFlowBox(s1, ' 用户发起秒杀请求', startX, 1.2, boxW, boxH, C.cardBg, C.dark);
addArrowRight(s1, startX + boxW, 1.2 + boxH/2, 0.5);
addFlowBox(s1, ' Redis Lua 脚本\n(原子操作)', startX + boxW + 0.5, 1.2, boxW, boxH, C.secondary, C.white, 11);

// 第二行：Lua 脚本内部逻辑（详细展开）
addArrowDown(s1, startX + boxW + 0.5 + boxW/2, 1.2 + boxH, 0.4);
addFlowBox(s1, '① 检查用户购买次数\n(HGET boughtKey userId)', startX + 0.3, 2.2, boxW, boxH, 'E0F2FE', C.primary, 10);
addArrowDown(s1, startX + 0.3 + boxW/2, 2.2 + boxH, 0.3);
addFlowBox(s1, '② 检查库存是否充足\n(GET stockKey)', startX + 0.3, 2.8, boxW, boxH, 'E0F2FE', C.primary, 10);
addArrowDown(s1, startX + 0.3 + boxW/2, 2.8 + boxH, 0.3);
addFlowBox(s1, '③ 扣减库存 + 记录购买\n(DECR + HINCRBY)', startX + 0.3, 3.4, boxW, boxH, C.success, C.white, 10);

// 判断分支
addArrowRight(s1, startX + boxW + 0.3, 2.5, 0.5);
addFlowBox(s1, '❌ 返回 -1\n(已达限购)', startX + boxW + 0.8, 2.35, 1.5, boxH, 'FEE2E2', C.error, 10);

addArrowRight(s1, startX + boxW + 0.3, 3.1, 0.5);
addFlowBox(s1, '❌ 返回 0\n(库存不足)', startX + boxW + 0.8, 2.95, 1.5, boxH, 'FEE2E2', C.error, 10);

addArrowRight(s1, startX + boxW + 0.3, 3.7, 0.5);
addFlowBox(s1, '✅ 返回 1\n(抢购成功)', startX + boxW + 0.8, 3.55, 1.5, boxH, 'D1FAE5', C.success, 10);

// 第三行：数据库事务
addArrowDown(s1, startX + boxW + 0.5 + boxW/2, 1.2 + boxH, arrowLen);
addFlowBox(s1, '🗄️ 数据库事务\n(乐观锁 + 创建订单)', startX + boxW + 0.5 + boxW - 1.1, 2.0, boxW, boxH, C.primary, C.white, 11);

// 第四行：返回结果
addArrowDown(s1, startX + boxW + 0.5 + boxW/2, 2.0 + boxH, arrowLen);
addFlowBox(s1, '🎯 返回订单号\n(orderNo + expireTime)', startX + boxW + 0.5, 2.8, boxW, boxH, C.accent, C.white, 11);

// 第五行：定时任务
addArrowDown(s1, startX + boxW + 0.5 + boxW/2, 2.8 + boxH, arrowLen);
addFlowBox(s1, '⏰ 定时任务\n(每30秒扫描超时订单)', startX + boxW + 0.5, 3.6, boxW, boxH, C.warning, C.white, 11);

// 第六行：回滚
addArrowDown(s1, startX + boxW + 0.5 + boxW/2, 3.6 + boxH, arrowLen);
addFlowBox(s1, ' 超时回滚\n(恢复DB+Redis库存)', startX + boxW + 0.5, 4.4, boxW, boxH, 'FEF3C7', C.warning, 11);

// 底部技术亮点
s1.addShape(pres.shapes.RECTANGLE, {
  x: 0.5, y: 5.15, w: 9, h: 0.35,
  fill: { color: 'ECFDF5' },
  line: { color: C.accent, width: 1 }
});
s1.addText('💡 核心优势：Lua脚本保证原子性 | 同步事务保证强一致性 | 定时任务兜底超时订单', {
  x: 0.7, y: 5.15, w: 8.6, h: 0.35,
  fontSize: 11, color: '065F46', fontFace: "Arial", margin: 0, valign: "middle"
});

// ========== Slide 2: Lua 脚本详解 ==========
let s2 = pres.addSlide();
s2.background = { color: C.light };
addSideBar(s2);
addTitle(s2, '秒杀系统 · Lua 脚本原子操作');

// 左侧：Lua 脚本代码框
s2.addShape(pres.shapes.RECTANGLE, {
  x: 0.3, y: 1.1, w: 4.5, h: 3.8,
  fill: { color: '1E293B' },
  line: { color: C.primary, width: 2 }
});

s2.addText('Lua Script (原子执行)', {
  x: 0.4, y: 1.15, w: 4.3, h: 0.4,
  fontSize: 14, bold: true, color: C.accent, fontFace: "Consolas", margin: 0
});

const codeLines = [
  { text: 'local stockKey = KEYS[1]', color: 'A5B4FC' },
  { text: 'local boughtKey = KEYS[2]', color: 'A5B4FC' },
  { text: 'local userId = ARGV[1]', color: 'A5B4FC' },
  { text: 'local limit = ARGV[2]', color: 'A5B4FC' },
  { text: '', color: 'FFFFFF' },
  { text: '--  检查一人一单', color: '6EE7B7' },
  { text: 'local bought = redis.call(\'HGET\',', color: 'E2E8F0' },
  { text: '    boughtKey, userId) or \'0\'', color: 'E2E8F0' },
  { text: 'if bought >= limit then', color: 'F472B6' },
  { text: '    return -1  -- 超限', color: 'FCA5A5' },
  { text: 'end', color: 'F472B6' },
  { text: '', color: 'FFFFFF' },
  { text: '-- ② 检查库存', color: '6EE7B7' },
  { text: 'local stock = redis.call(\'GET\',', color: 'E2E8F0' },
  { text: '    stockKey)', color: 'E2E8F0' },
  { text: 'if not stock or stock <= 0 then', color: 'F472B6' },
  { text: '    return 0   -- 无库存', color: 'FCA5A5' },
  { text: 'end', color: 'F472B6' },
  { text: '', color: 'FFFFFF' },
  { text: '-- ③ 扣库存 + 记录购买', color: '6EE7B7' },
  { text: 'redis.call(\'DECR\', stockKey)', color: 'E2E8F0' },
  { text: 'redis.call(\'HINCRBY\',', color: 'E2E8F0' },
  { text: '    boughtKey, userId, 1)', color: 'E2E8F0' },
  { text: 'return 1  -- 成功', color: '6EE7B7' },
];

codeLines.forEach((line, i) => {
  s2.addText(line.text, {
    x: 0.5, y: 1.6 + i * 0.13, w: 4.2, h: 0.15,
    fontSize: 10, color: line.color, fontFace: "Consolas", margin: 0
  });
});

// 右侧：Redis 数据结构说明
s2.addShape(pres.shapes.RECTANGLE, {
  x: 5.2, y: 1.1, w: 4.3, h: 1.8,
  fill: { color: C.cardBg },
  line: { color: C.border, width: 1 },
  shadow: { type: "outer", color: "000000", blur: 4, offset: 1, angle: 135, opacity: 0.1 }
});

s2.addText(' Redis 数据结构', {
  x: 5.3, y: 1.15, w: 4.1, h: 0.4,
  fontSize: 14, bold: true, color: C.primary, fontFace: "Arial", margin: 0
});

s2.addText([
  { text: '• seckill:stock:{id}', options: { breakLine: true, bold: true } },
  { text: '  类型: RAtomicLong', options: { breakLine: true } },
  { text: '  用途: 实时库存计数', options: { breakLine: true } },
  { text: '', options: { breakLine: true } },
  { text: '• seckill:bought:{id}', options: { breakLine: true, bold: true } },
  { text: '  类型: Hash', options: { breakLine: true } },
  { text: '  用途: 记录每个用户的购买次数', options: { breakLine: false } },
], {
  x: 5.4, y: 1.6, w: 4, h: 1.2,
  fontSize: 11, color: C.body, fontFace: "Arial", margin: 0
});

// 优势说明
s2.addShape(pres.shapes.RECTANGLE, {
  x: 5.2, y: 3.1, w: 4.3, h: 1.8,
  fill: { color: C.cardBg },
  line: { color: C.border, width: 1 },
  shadow: { type: "outer", color: "000000", blur: 4, offset: 1, angle: 135, opacity: 0.1 }
});

s2.addText('✨ Lua 脚本优势', {
  x: 5.3, y: 3.15, w: 4.1, h: 0.4,
  fontSize: 14, bold: true, color: C.primary, fontFace: "Arial", margin: 0
});

s2.addText([
  { text: ' 原子性: 所有操作一次性执行', options: { breakLine: true } },
  { text: '② 无并发窗口: 避免多次网络请求', options: { breakLine: true } },
  { text: ' 高性能: 减少 Redis 往返次数', options: { breakLine: true } },
  { text: '④ 代码简洁: 逻辑集中在一个脚本', options: { breakLine: false } },
], {
  x: 5.4, y: 3.6, w: 4, h: 1.2,
  fontSize: 11, color: C.body, fontFace: "Arial", margin: 0
});

// 底部高亮
s2.addShape(pres.shapes.RECTANGLE, {
  x: 0.3, y: 5.1, w: 9.2, h: 0.4,
  fill: { color: 'DBEAFE' },
  line: { color: C.primary, width: 1 }
});
s2.addText(' 关键代码: SeckillServiceImpl.java 第 58-77 行 (SECKILL_LUA 常量)', {
  x: 0.5, y: 5.1, w: 8.8, h: 0.4,
  fontSize: 12, bold: true, color: C.primary, fontFace: "Arial", margin: 0, valign: "middle"
});

// ========== Slide 3: 定时任务与回滚 ==========
let s3 = pres.addSlide();
s3.background = { color: C.light };
addSideBar(s3);
addTitle(s3, '秒杀系统 · 定时任务与回滚机制');

// 左侧：定时任务流程
s3.addShape(pres.shapes.RECTANGLE, {
  x: 0.3, y: 1.1, w: 4.5, h: 3.5,
  fill: { color: C.cardBg },
  line: { color: C.border, width: 1 },
  shadow: { type: "outer", color: "000000", blur: 4, offset: 1, angle: 135, opacity: 0.1 }
});

s3.addText('⏰ 定时任务流程', {
  x: 0.4, y: 1.15, w: 4.3, h: 0.4,
  fontSize: 16, bold: true, color: C.primary, fontFace: "Arial", margin: 0
});

const scheduledSteps = [
  { icon: '①', text: '@Scheduled(cron = "*/30 * * * * ?")', detail: '每30秒执行一次' },
  { icon: '②', text: '查询超时订单', detail: 'WHERE status=\'PENDING_PAY\' AND expire_time < NOW()' },
  { icon: '', text: '更新订单状态', detail: 'PENDING_PAY → EXPIRED' },
  { icon: '④', text: '回滚数据库库存', detail: 'book.stock = stock + 1' },
  { icon: '⑤', text: '回滚Redis库存', detail: 'RAtomicLong.incrementAndGet()' },
  { icon: '⑥', text: '回滚已售数量', detail: 'activity.sold_count = sold_count - 1' },
];

scheduledSteps.forEach((step, i) => {
  const y = 1.7 + i * 0.48;
  s3.addShape(pres.shapes.OVAL, {
    x: 0.5, y, w: 0.35, h: 0.35,
    fill: { color: C.secondary }
  });
  s3.addText(step.icon, {
    x: 0.5, y, w: 0.35, h: 0.35,
    fontSize: 12, bold: true, color: C.white, fontFace: "Arial", margin: 0, valign: "middle", align: "center"
  });
  s3.addText(step.text, {
    x: 1.0, y: y + 0.05, w: 3.5, h: 0.2,
    fontSize: 11, bold: true, color: C.dark, fontFace: "Arial", margin: 0
  });
  s3.addText(step.detail, {
    x: 1.0, y: y + 0.25, w: 3.5, h: 0.15,
    fontSize: 9, color: C.muted, fontFace: "Consolas", margin: 0
  });
});

// 右侧：回滚代码示例
s3.addShape(pres.shapes.RECTANGLE, {
  x: 5.2, y: 1.1, w: 4.3, h: 2.8,
  fill: { color: '1E293B' },
  line: { color: C.warning, width: 2 }
});

s3.addText('回滚代码片段', {
  x: 5.3, y: 1.15, w: 4.1, h: 0.4,
  fontSize: 14, bold: true, color: C.warning, fontFace: "Consolas", margin: 0
});

const rollbackCode = [
  { text: 'private void rollback(', color: 'A5B4FC' },
  { text: '  SeckillOrder order,', color: 'E2E8F0' },
  { text: '  String targetStatus) {', color: 'E2E8F0' },
  { text: '', color: 'FFFFFF' },
  { text: '  // 1. 更新订单状态', color: '6EE7B7' },
  { text: '  seckillOrderMapper.update(...)', color: 'E2E8F0' },
  { text: '    .set(status, targetStatus)', color: 'E2E8F0' },
  { text: '', color: 'FFFFFF' },
  { text: '  // 2. 恢复DB库存', color: '6EE7B7' },
  { text: '  bookMapper.update(...)', color: 'E2E8F0' },
  { text: '    .setSql("stock = stock + 1")', color: 'E2E8F0' },
  { text: '', color: 'FFFFFF' },
  { text: '  // 3. 恢复Redis库存', color: '6EE7B7' },
  { text: '  RAtomicLong stock =', color: 'E2E8F0' },
  { text: '    redissonClient.getAtomicLong(...)', color: 'E2E8F0' },
  { text: '  stock.incrementAndGet()', color: 'E2E8F0' },
  { text: '}', color: 'A5B4FC' },
];

rollbackCode.forEach((line, i) => {
  s3.addText(line.text, {
    x: 5.4, y: 1.6 + i * 0.13, w: 4, h: 0.15,
    fontSize: 9, color: line.color, fontFace: "Consolas", margin: 0
  });
});

// 底部：关键特性
s3.addShape(pres.shapes.RECTANGLE, {
  x: 0.3, y: 4.8, w: 9.2, h: 0.7,
  fill: { color: 'FEF3C7' },
  line: { color: C.warning, width: 1 }
});

s3.addText(' 幂等性保证：只有 PENDING_PAY 状态的订单才会被回滚，避免重复回滚', {
  x: 0.5, y: 4.85, w: 8.8, h: 0.25,
  fontSize: 12, bold: true, color: '92400E', fontFace: "Arial", margin: 0
});

s3.addText('📌 关键代码: SeckillServiceImpl.java 第 274-330 行 (autoExpire + rollback)', {
  x: 0.5, y: 5.15, w: 8.8, h: 0.25,
  fontSize: 11, color: '92400E', fontFace: "Arial", margin: 0
});

// 保存文件
pres.writeFile({ fileName: "秒杀系统流程图.pptx" });
console.log('✅ 秒杀系统流程图已生成: 秒杀系统流程图.pptx');
