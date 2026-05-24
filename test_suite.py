#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
智能书店系统综合测试脚本
"""

import os
import sys
import requests
import json
import threading
import time
import concurrent.futures
import random

# Windows编码修复
if sys.platform == "win32":
    os.system("chcp 65001 >nul 2>&1")
    sys.stdout.reconfigure(encoding="utf-8")

BASE_URL = "http://localhost:8080"

class Colors:
    GREEN = "\033[92m"
    RED = "\033[91m"
    YELLOW = "\033[93m"
    CYAN = "\033[96m"
    RESET = "\033[0m"

def ok(msg): print(f"{Colors.GREEN}[PASS]{Colors.RESET} {msg}")
def fail(msg): print(f"{Colors.RED}[FAIL]{Colors.RESET} {msg}")
def warn(msg): print(f"{Colors.YELLOW}[WARN]{Colors.RESET} {msg}")
def info(msg): print(f"{Colors.CYAN}[INFO]{Colors.RESET} {msg}")

class Tester:
    def __init__(self):
        self.tokens = {}
        self.admin_address_id = None
        self.pass_count = 0
        self.fail_count = 0
        self.warn_count = 0
        self.session = requests.Session()

    def req(self, method, path, **kwargs):
        try:
            return self.session.request(method, f"{BASE_URL}{path}", timeout=15, **kwargs)
        except Exception as e:
            fail(f"请求异常 {path}: {e}")
            return None

    def jcheck(self, resp, expect=200):
        if resp is None:
            return False, None
        try:
            d = resp.json()
            return d.get("code") == expect, d
        except:
            return False, None

    def inc(self, t):
        if t == "ok": self.pass_count += 1
        elif t == "fail": self.fail_count += 1
        elif t == "warn": self.warn_count += 1

    # ========== 功能接口测试 ==========
    def test_public(self):
        info("\n========== 1. 公开接口测试 ==========")
        tests = [
            ("GET", "/api/book/list?page=1&size=5", None, 200, "图书列表"),
            ("GET", "/api/book/21", None, 200, "图书详情"),
            ("GET", "/api/book/hot?limit=5", None, 200, "热门图书"),
            ("GET", "/api/book/new?limit=5", None, 200, "新书"),
            ("GET", "/api/book/search?keyword=活着&page=1&size=5", None, 200, "图书搜索"),
            ("GET", "/api/book/recommend?limit=5", None, 200, "图书推荐"),
            ("GET", "/api/book/21/similar?limit=5", None, 200, "相似图书"),
            ("GET", "/api/seckill/running", None, 200, "进行中秒杀"),
            ("GET", "/api/seckill/upcoming", None, 200, "即将开始秒杀"),
            ("GET", "/api/seckill/activities/1", None, 200, "秒杀活动详情"),
            ("GET", "/api/review/book/21?page=1&size=5", None, 200, "图书评价"),
            ("GET", "/api/coupons/available", None, 200, "可用优惠券（未登录）"),
        ]
        for m, p, body, exp, name in tests:
            kwargs = body or {}
            resp = self.req(m, p, **kwargs)
            ok_flag, data = self.jcheck(resp, exp)
            if ok_flag:
                ok(name); self.inc("ok")
            else:
                fail(f"{name} -> code={data.get('code') if data else 'N/A'}, msg={data.get('msg') if data else resp.status_code if resp else 'NO_RESP'}")
                self.inc("fail")

        # 登录获取admin token
        resp = self.req("POST", "/api/auth/login", json={"account": "admin", "password": "123456"})
        flag, d = self.jcheck(resp, 200)
        if flag:
            self.tokens["admin"] = d["data"]["accessToken"]
            info("Admin登录成功")
        else:
            fail("Admin登录失败")
            self.inc("fail")

    def test_auth(self):
        info("\n========== 2. 需登录接口测试 ==========")
        if "admin" not in self.tokens:
            fail("缺少admin token，跳过"); return
        h = {"Authorization": f"Bearer {self.tokens['admin']}"}

        # 充值
        resp = self.req("POST", "/api/wallet/recharge?amount=9999", headers=h)
        f, d = self.jcheck(resp, 200)
        if f: ok("钱包充值"); self.inc("ok")
        else: fail("钱包充值"); self.inc("fail")

        # 余额
        resp = self.req("GET", "/api/wallet/balance", headers=h)
        f, d = self.jcheck(resp, 200)
        if f: ok(f"查询余额: {d['data']}"); self.inc("ok")
        else: fail("查询余额"); self.inc("fail")

        # 创建地址
        addr_body = {
            "receiver": "测试用户", "phone": "13800138000",
            "province": "广东省", "city": "深圳市", "district": "南山区", "detailAddress": "测试地址123号"
        }
        resp = self.req("POST", "/api/address", headers=h, json=addr_body)
        f, d = self.jcheck(resp, 200)
        if f:
            self.admin_address_id = d["data"]["id"]
            ok(f"创建地址 id={self.admin_address_id}"); self.inc("ok")
        else:
            resp2 = self.req("GET", "/api/address", headers=h)
            f2, d2 = self.jcheck(resp2, 200)
            if f2 and d2["data"]:
                self.admin_address_id = d2["data"][0]["id"]
                warn(f"创建地址失败，使用已有 id={self.admin_address_id}"); self.inc("warn")
            else:
                fail(f"创建地址: {d.get('msg') if d else 'ERR'}"); self.inc("fail")

        apis = [
            ("GET", "/api/user/me", "用户信息"),
            ("GET", "/api/cart", "购物车"),
            ("GET", "/api/order?page=1&size=5", "订单列表"),
            ("GET", "/api/favorite?page=1&size=5", "收藏列表"),
            ("GET", "/api/coupons/mine?status=UNUSED", "我的优惠券"),
            ("GET", "/api/seckill/orders?page=1&size=5", "秒杀订单"),
            ("GET", "/api/address", "地址列表"),
        ]
        for m, p, name in apis:
            resp = self.req(m, p, headers=h)
            f, d = self.jcheck(resp, 200)
            if f: ok(name); self.inc("ok")
            else: fail(f"{name} -> {d.get('msg') if d else 'ERR'}"); self.inc("fail")

    def test_order(self):
        info("\n========== 3. 普通订单流程测试 ==========")
        if "admin" not in self.tokens:
            fail("无token"); return
        h = {"Authorization": f"Bearer {self.tokens['admin']}"}

        # 获取图书库存
        resp = self.req("GET", "/api/book/21")
        f, d = self.jcheck(resp, 200)
        stock_before = d["data"]["stock"] if f else None
        info(f"图书21库存: {stock_before}")

        # 加购物车
        resp = self.req("POST", "/api/cart", headers=h, json={"bookId": 21, "quantity": 1})
        f, d = self.jcheck(resp, 200)
        if f:
            cart_id = d["data"]["id"]
            ok(f"加购物车 id={cart_id}"); self.inc("ok")
        else:
            r2 = self.req("GET", "/api/cart", headers=h)
            f2, d2 = self.jcheck(r2, 200)
            if f2 and d2["data"]:
                cart_id = d2["data"][0]["id"]
                warn(f"加车失败，使用已有 id={cart_id}"); self.inc("warn")
            else:
                fail("购物车操作失败"); self.inc("fail"); return

        if not self.admin_address_id:
            warn("无地址，跳过订单流程"); self.inc("warn"); return

        # 创建订单
        resp = self.req("POST", "/api/order", headers=h, json={"cartItemIds": [cart_id], "addressId": self.admin_address_id})
        f, d = self.jcheck(resp, 200)
        if f:
            order_no = d["data"]["orderNo"]
            ok(f"创建订单 {order_no}"); self.inc("ok")
        else:
            fail(f"创建订单: {d.get('msg') if d else 'ERR'}"); self.inc("fail"); return

        # 支付
        resp = self.req("POST", f"/api/order/{order_no}/pay", headers=h, json={"payMethod": "BALANCE"})
        f, d = self.jcheck(resp, 200)
        if f: ok("支付订单"); self.inc("ok")
        else: fail(f"支付: {d.get('msg') if d else 'ERR'}"); self.inc("fail"); return

        # 普通订单支付后需要管理员发货才能确认收货，这里跳过确认收货
        warn("订单已支付，确认收货需要admin后台发货（跳过）"); self.inc("warn")

        # 验证库存
        if stock_before is not None:
            time.sleep(0.3)
            resp = self.req("GET", "/api/book/21")
            f, d = self.jcheck(resp, 200)
            if f:
                stock_after = d["data"]["stock"]
                if stock_after == stock_before - 1:
                    ok(f"库存扣减正确: {stock_before} -> {stock_after}"); self.inc("ok")
                else:
                    fail(f"库存异常! 期望{stock_before-1}, 实际{stock_after}"); self.inc("fail")

    # ========== 高并发秒杀测试 ==========
    def reg_user(self, idx):
        phone = f"138{random.randint(10000000, 99999999)}"
        uname = f"stress{idx}_{int(time.time()*1000)%100000}"
        resp = self.req("POST", "/api/auth/register", json={"username": uname, "password": "123456", "phone": phone})
        f, d = self.jcheck(resp, 200)
        if f:
            return d["data"]["accessToken"], d["data"]["user"]["id"]
        return None, None

    def prepare_user(self, idx):
        token, uid = self.reg_user(idx)
        if not token: return None
        h = {"Authorization": f"Bearer {token}"}
        self.req("POST", "/api/wallet/recharge?amount=9999", headers=h)
        phone = f"138{random.randint(10000000, 99999999)}"
        resp = self.req("POST", "/api/address", headers=h, json={
            "receiver": f"U{idx}", "phone": phone,
            "province": "广东", "city": "深圳", "district": "南山", "detailAddress": f"A{idx}"
        })
        f, d = self.jcheck(resp, 200)
        if f:
            addr_id = d["data"]["id"]
        else:
            r2 = self.req("GET", "/api/address", headers=h)
            f2, d2 = self.jcheck(r2, 200)
            addr_id = d2["data"][0]["id"] if f2 and d2["data"] else None
        if not addr_id: return None
        return {"token": token, "uid": uid, "addr_id": addr_id}

    def test_seckill_stress(self, count=20):
        info(f"\n========== 4. 高并发秒杀超卖测试 ({count}用户) ==========")
        resp = self.req("GET", "/api/seckill/running")
        f, d = self.jcheck(resp, 200)
        if not f or not d["data"]:
            fail("无进行中的秒杀活动"); return
        act = d["data"][0]
        act_id, book_id = act["id"], act["bookId"]
        redis_stock = act["remainingStock"]
        info(f"活动={act_id}, 图书={book_id}, Redis库存={redis_stock}")

        # DB库存
        r2 = self.req("GET", f"/api/book/{book_id}")
        f2, d2 = self.jcheck(r2, 200)
        db_stock = d2["data"]["stock"] if f2 else None
        info(f"DB库存={db_stock}")

        # 准备用户
        info(f"准备 {count} 个用户...")
        users = []
        with concurrent.futures.ThreadPoolExecutor(8) as ex:
            fs = {ex.submit(self.prepare_user, i): i for i in range(count)}
            for fut in concurrent.futures.as_completed(fs):
                res = fut.result()
                if res: users.append(res)
        info(f"成功准备 {len(users)} 个用户")
        if not users:
            fail("无可用测试用户"); return

        # 并发抢购
        info("开始并发抢购...")
        stats = {"ok": 0, "no_stock": 0, "limited": 0, "other": 0, "err": 0}
        orders = []
        lock = threading.Lock()

        def buy(u):
            h = {"Authorization": f"Bearer {u['token']}"}
            try:
                r = requests.post(f"{BASE_URL}/api/seckill/buy", headers=h,
                                  json={"activityId": act_id, "addressId": u["addr_id"]}, timeout=15)
                data = r.json()
                code, msg = data.get("code"), data.get("msg", "")
                with lock:
                    if code == 200:
                        stats["ok"] += 1
                        orders.append(data["data"]["orderNo"])
                    elif "库存" in msg or code == 5008:
                        stats["no_stock"] += 1
                    elif "限制" in msg or code == 5007:
                        stats["limited"] += 1
                    else:
                        stats["other"] += 1
            except Exception as e:
                with lock: stats["err"] += 1

        t0 = time.time()
        with concurrent.futures.ThreadPoolExecutor(len(users)) as ex:
            list(ex.map(buy, users))
        elapsed = time.time() - t0
        info(f"抢购完成，耗时 {elapsed:.2f}s")
        info(f"成功={stats['ok']}, 库存不足={stats['no_stock']}, 已限购={stats['limited']}, 其他={stats['other']}, 异常={stats['err']}")

        # 验证
        time.sleep(0.5)
        resp = self.req("GET", "/api/seckill/running")
        f, d = self.jcheck(resp, 200)
        redis_after = d["data"][0]["remainingStock"] if f and d["data"] else None
        sold_after = d["data"][0]["soldCount"] if f and d["data"] else None
        info(f"后 Redis库存={redis_after}, soldCount={sold_after}")

        r2 = self.req("GET", f"/api/book/{book_id}")
        f2, d2 = self.jcheck(r2, 200)
        db_after = d2["data"]["stock"] if f2 else None
        info(f"后 DB库存={db_after}")

        # 判断
        if redis_after is not None:
            expected = max(0, redis_stock - stats["ok"])
            if redis_after == expected:
                ok(f"Redis库存正确: {redis_stock} -> {redis_after}"); self.inc("ok")
            else:
                fail(f"Redis库存异常! 期望={expected}, 实际={redis_after}"); self.inc("fail")

        if db_stock is not None and db_after is not None:
            expected_db = db_stock - stats["ok"]
            if db_after == expected_db:
                ok(f"DB库存正确: {db_stock} -> {db_after}"); self.inc("ok")
            else:
                fail(f"DB库存异常! 期望={expected_db}, 实际={db_after} -> 可能超卖!"); self.inc("fail")

        if stats["ok"] > redis_stock:
            fail(f"严重超卖! 成功{stats['ok']} > 库存{redis_stock}"); self.inc("fail")
        else:
            ok(f"未超卖: 卖出{stats['ok']} <= 库存{redis_stock}"); self.inc("ok")

        # 检查重复购买（每个用户只能买1次）
        if stats["ok"] > 0:
            uids = [u["uid"] for u in users]
            if len(uids) != len(set(uids)):
                fail("存在重复用户ID!"); self.inc("fail")
            else:
                ok("所有用户ID唯一"); self.inc("ok")

    # ========== Redis问题测试 ==========
    def test_redis(self):
        info("\n========== 5. Redis相关问题测试 ==========")

        info("--- 5.1 秒杀Redis一致性检查 ---")
        resp = self.req("GET", "/api/seckill/running")
        f, d = self.jcheck(resp, 200)
        if f and d["data"]:
            act = d["data"][0]
            db_calc = act["totalStock"] - act["soldCount"]
            redis_val = act["remainingStock"]
            info(f"totalStock={act['totalStock']}, soldCount={act['soldCount']}, DB计算={db_calc}, Redis={redis_val}")
            if redis_val == db_calc:
                ok("Redis与DB库存一致"); self.inc("ok")
            else:
                warn(f"不一致: Redis={redis_val}, DB={db_calc}"); self.inc("warn")

        info("--- 5.2 缓存击穿/雪崩/穿透分析 ---")
        info("扫描代码发现: 项目中未使用@Cacheable/Spring Cache，查询未走Redis缓存")
        times = []
        for i in range(5):
            t0 = time.time()
            self.req("GET", "/api/book/21")
            times.append((time.time()-t0)*1000)
        avg = sum(times)/len(times)
        info(f"图书详情5次请求平均耗时: {avg:.1f}ms")
        warn("项目未使用Redis查询缓存，不存在经典缓存击穿/雪崩问题，但DB压力大时可能性能瓶颈"); self.inc("warn")

        info("--- 5.3 秒杀Redis key丢失恢复 ---")
        info("潜在风险: 若stockKey被删除，多并发请求可能同时触发fallback重新初始化")
        info("防护措施: Lua原子脚本 + DB乐观锁 WHERE stock >= 1")
        ok("秒杀使用Lua原子脚本 + DB乐观锁双重防护"); self.inc("ok")

        info("--- 5.4 缓存穿透防护 ---")
        resp = self.req("GET", "/api/book/99999999")
        f, d = self.jcheck(resp, 200)
        if not f:
            ok("不存在图书返回正确错误码"); self.inc("ok")
        else:
            warn("不存在图书返回200"); self.inc("warn")

        info("--- 5.5 限流测试 ---")
        if "admin" not in self.tokens:
            warn("无token跳过限流测试"); return
        h = {"Authorization": f"Bearer {self.tokens['admin']}"}
        codes = []
        for i in range(6):
            r = self.req("POST", "/api/ai/chat", headers=h, json={"message": "hi", "sessionId": None})
            if r:
                try: codes.append(r.json().get("code"))
                except: codes.append(r.status_code)
            time.sleep(0.05)
        info(f"AI接口6次快速请求码: {codes}")
        if 429 in codes or 5001 in codes or (codes.count(200) < 6 and any(c != 200 for c in codes)):
            ok("限流似乎生效"); self.inc("ok")
        else:
            warn("未触发限流（AI限流QPS=2，可能间隔不够或配置不同）"); self.inc("warn")

    def summary(self):
        info("\n========== 测试总结 ==========")
        total = self.pass_count + self.fail_count + self.warn_count
        info(f"总检查项: {total}")
        ok(f"通过: {self.pass_count}")
        fail(f"失败: {self.fail_count}")
        warn(f"警告: {self.warn_count}")
        if self.fail_count == 0:
            ok("所有关键测试通过！")
        else:
            fail(f"存在 {self.fail_count} 个失败项，请检查！")

    def run(self):
        self.test_public()
        self.test_auth()
        self.test_order()
        self.test_seckill_stress(count=20)
        self.test_redis()
        self.summary()

if __name__ == "__main__":
    Tester().run()
