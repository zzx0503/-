// 后台管理 - 通用工具与侧边栏渲染

const ADMIN_TOKEN = localStorage.getItem('accessToken');
if (!ADMIN_TOKEN) {
  window.location.href = '../index.html';
}

const ADMIN_NAV = [
  { key: 'index',      icon: '📊', label: '仪表盘',     href: 'index.html' },
  { key: 'books',      icon: '📚', label: '图书管理',   href: 'books.html' },
  { key: 'categories', icon: '🗂',  label: '分类管理',   href: 'categories.html' },
  { key: 'orders',     icon: '📦', label: '订单管理',   href: 'orders.html' },
  { key: 'coupons',    icon: '🎫', label: '优惠券',     href: 'coupons.html' },
  { key: 'seckill',    icon: '⚡️',  label: '秒杀活动',   href: 'seckill.html' },
  { key: 'logs',       icon: '📝', label: '操作日志',   href: 'logs.html' },
];

let __activeKey = 'index';

function setActiveNav(key) {
  __activeKey = key;
  renderSideNav();
}

function renderSideNav() {
  const nav = document.getElementById('adminSideNav');
  if (!nav) return;
  nav.innerHTML = ADMIN_NAV.map(it => `
    <a class="nav-item ${it.key === __activeKey ? 'active' : ''}" href="${it.href}">
      <span>${it.icon}</span>
      <span>${it.label}</span>
    </a>
  `).join('') + `
    <a class="nav-item" href="../home.html" style="margin-top:18px;border-top:1px dashed var(--border);padding-top:14px">
      <span>🏠</span>
      <span>返回前台</span>
    </a>
  `;
}

async function api(path, opts) {
  opts = opts || {};
  opts.headers = opts.headers || {};
  opts.headers['Authorization'] = 'Bearer ' + ADMIN_TOKEN;
  try {
    const res = await fetch(path, opts);
    const text = await res.text();
    try { return JSON.parse(text); }
    catch (e) { return { code: res.status, msg: text || '响应解析失败' }; }
  } catch (e) {
    return { code: -1, msg: '网络错误：' + e.message };
  }
}

async function loadUser() {
  renderSideNav();
  const result = await api('/api/user/me');
  if (result.code !== 200) {
    logout();
    return;
  }
  const d = result.data;
  if (d.role !== 'ADMIN') {
    alert('您不是管理员，无法访问后台。');
    window.location.href = '../home.html';
    return;
  }
  const nameEl = document.getElementById('sideName');
  const roleEl = document.getElementById('sideRole');
  if (nameEl) nameEl.textContent = d.nickname || d.username;
  if (roleEl) roleEl.textContent = d.role || 'ADMIN';
}

function escapeHtml(s) {
  if (s == null) return '';
  return String(s)
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#39;');
}

function fmtMoney(n) {
  return '¥' + Number(n || 0).toFixed(2);
}

function fmtDateTime(s) {
  if (!s) return '--';
  return String(s).replace('T', ' ').slice(0, 19);
}

function logout() {
  localStorage.clear();
  window.location.href = '../index.html';
}

function showToast(msg, type) {
  const t = document.createElement('div');
  t.className = 'admin-toast ' + (type === 'err' ? 'err' : 'ok');
  t.textContent = msg;
  document.body.appendChild(t);
  setTimeout(() => t.classList.add('show'), 10);
  setTimeout(() => {
    t.classList.remove('show');
    setTimeout(() => t.remove(), 300);
  }, 1800);
}

// 通用 toast 样式（一次性注入）
(function injectToastStyle() {
  if (document.getElementById('admin-toast-style')) return;
  const s = document.createElement('style');
  s.id = 'admin-toast-style';
  s.textContent = `
    .admin-toast {
      position: fixed;
      top: 28px;
      left: 50%;
      transform: translate(-50%, -16px);
      background: #333;
      color: #fff;
      padding: 10px 22px;
      border-radius: 22px;
      font-size: 14px;
      opacity: 0;
      transition: opacity .3s, transform .3s;
      z-index: 9999;
      pointer-events: none;
      box-shadow: 0 6px 24px rgba(0,0,0,0.25);
    }
    .admin-toast.show { opacity: 1; transform: translate(-50%, 0); }
    .admin-toast.err { background: #c0392b; }
    .admin-toast.ok  { background: #27ae60; }

    /* ===== 后台通用表格 ===== */
    .admin-table {
      width: 100%;
      border-collapse: collapse;
      background: var(--white);
      border-radius: var(--radius);
      overflow: hidden;
      box-shadow: var(--shadow);
      border: 1px solid var(--border);
    }
    .admin-table th, .admin-table td {
      padding: 12px 14px;
      text-align: left;
      font-size: 13px;
      border-bottom: 1px solid var(--border);
    }
    .admin-table th {
      background: #fff8f1;
      color: var(--text-secondary);
      font-weight: 600;
      white-space: nowrap;
    }
    .admin-table tr:last-child td { border-bottom: none; }
    .admin-table tr:hover td { background: #fffbf5; }

    /* ===== 通用按钮 ===== */
    .admin-btn {
      padding: 6px 14px;
      border: 1px solid var(--border);
      border-radius: 6px;
      background: var(--white);
      font-size: 13px;
      cursor: pointer;
      color: var(--text-main);
    }
    .admin-btn:hover { border-color: var(--primary); color: var(--primary); }
    .admin-btn.primary { background: var(--primary); color: var(--white); border-color: var(--primary); }
    .admin-btn.primary:hover { opacity: .9; color: var(--white); }
    .admin-btn.danger  { color: #c0392b; border-color: #f5c6cb; }
    .admin-btn.danger:hover { background: #fff0f0; color: #c0392b; }
    .admin-btn.ghost { background: transparent; }
    .admin-btn:disabled { opacity: .5; cursor: not-allowed; }

    /* ===== 通用工具栏 ===== */
    .admin-toolbar {
      display: flex;
      gap: 10px;
      align-items: center;
      flex-wrap: wrap;
      margin-bottom: 14px;
    }
    .admin-toolbar input[type=text],
    .admin-toolbar input[type=number],
    .admin-toolbar input[type=date],
    .admin-toolbar select {
      padding: 7px 10px;
      border: 1px solid var(--border);
      border-radius: 6px;
      font-size: 13px;
      outline: none;
      background: var(--white);
    }
    .admin-toolbar input:focus, .admin-toolbar select:focus { border-color: var(--primary); }
    .admin-toolbar .grow { flex: 1; }

    /* ===== 通用 modal ===== */
    .admin-mask {
      position: fixed; inset: 0;
      background: rgba(0,0,0,.45);
      display: none;
      align-items: center; justify-content: center;
      z-index: 1000;
    }
    .admin-mask.active { display: flex; }
    .admin-dialog {
      background: var(--white);
      border-radius: var(--radius);
      width: 540px;
      max-width: 92vw;
      max-height: 88vh;
      overflow: auto;
      box-shadow: var(--shadow);
      padding: 22px 26px;
    }
    .admin-dialog h3 { margin-bottom: 16px; font-size: 17px; }
    .admin-form-row {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 12px 14px;
      margin-bottom: 12px;
    }
    .admin-form-row.full { grid-template-columns: 1fr; }
    .admin-form-row label {
      display: block;
      font-size: 12px;
      color: var(--text-secondary);
      margin-bottom: 4px;
    }
    .admin-form-row input, .admin-form-row select, .admin-form-row textarea {
      width: 100%;
      padding: 8px 10px;
      border: 1px solid var(--border);
      border-radius: 6px;
      font-size: 13px;
      outline: none;
      box-sizing: border-box;
      background: var(--white);
      font-family: inherit;
    }
    .admin-form-row textarea { min-height: 80px; resize: vertical; }
    .admin-form-row input:focus, .admin-form-row select:focus, .admin-form-row textarea:focus {
      border-color: var(--primary);
    }
    .admin-dialog .actions {
      display: flex;
      justify-content: flex-end;
      gap: 8px;
      margin-top: 14px;
    }

    /* ===== 状态徽标 ===== */
    .tag {
      display: inline-block;
      padding: 2px 8px;
      border-radius: 10px;
      font-size: 12px;
      font-weight: 600;
    }
    .tag.green { background: #e8f5e9; color: #27ae60; }
    .tag.red   { background: #fdecec; color: #c0392b; }
    .tag.gray  { background: #f0f0f0; color: #666; }
    .tag.blue  { background: #e7f1ff; color: #1d6fff; }
    .tag.yellow{ background: #fff7e0; color: #c8932a; }
    .tag.purple{ background: #f0e6ff; color: #7b3fbf; }

    /* ===== 通用分页 ===== */
    .admin-pagi {
      display: flex;
      justify-content: center;
      gap: 6px;
      margin-top: 16px;
    }
    .admin-pagi button {
      padding: 6px 12px;
      border: 1px solid var(--border);
      background: var(--white);
      border-radius: 6px;
      cursor: pointer;
      font-size: 13px;
    }
    .admin-pagi button.cur { background: var(--primary); color: #fff; border-color: var(--primary); }
    .admin-pagi button:disabled { opacity: .5; cursor: not-allowed; }
  `;
  document.head.appendChild(s);
})();
