import json

from langchain.tools import StructuredTool
from app.client import java_client
from app.agent.web_search import search_web, fetch_url


def _fmt_book_list(books: list, max_items: int = 15) -> str:
    """Format a list of book dicts for LLM consumption."""
    if not books:
        return "未找到匹配的图书。"
    lines = [f"共 {len(books)} 本图书:"]
    for b in books[:max_items]:
        if isinstance(b, dict):
            lines.append(
                f"  ID:{b.get('id')} 《{b.get('title')}》"
                f" 作者:{b.get('author', '')}"
                f" 价格:¥{b.get('price', '')}"
                f" 简介:{(b.get('description') or '')[:150]}"
            )
    if len(books) > max_items:
        lines.append(f"  ... 还有 {len(books) - max_items} 本未列出")
    return "\n".join(lines)


def _fmt_book_detail(d: dict) -> str:
    """Format a single book detail dict for LLM consumption."""
    if not d or not isinstance(d, dict):
        return "未找到该图书。"
    lines = [
        f"ID: {d.get('id')}",
        f"书名: 《{d.get('title')}》",
        f"作者: {d.get('author', '未知')}",
        f"出版社: {d.get('publisher', '未知')}",
        f"出版日期: {d.get('publishDate', '未知')}",
        f"价格: ¥{d.get('price', '')}",
        f"评分: {d.get('rating', '暂无')}",
        f"销量: {d.get('salesCount', 0)}",
        f"简介: {d.get('description') or '暂无简介'}",
    ]
    return "\n".join(lines)


def _fmt_user_profile(d: dict) -> str:
    """Format user profile for LLM consumption."""
    if not d or not isinstance(d, dict):
        return "用户暂无历史数据。"
    lines = ["用户画像:"]
    if d.get("favoriteTitles"):
        lines.append(f"  收藏的图书: {', '.join(str(t) for t in d['favoriteTitles'])}")
    if d.get("searchKeywords"):
        lines.append(f"  搜索历史: {', '.join(str(k) for k in d['searchKeywords'])}")
    if d.get("orderCount"):
        lines.append(f"  历史订单数: {d['orderCount']}")
    if d.get("preferredCategories"):
        lines.append(f"  偏好分类: {', '.join(str(c) for c in d['preferredCategories'])}")
    if len(lines) == 1:
        return "用户暂无历史数据。"
    return "\n".join(lines)


def search_books(keyword: str, limit: int = 10) -> str:
    """按关键词从书店数据库搜索图书（匹配书名、作者、简介、分类）。返回图书列表含ID、书名、作者、价格、简介摘要。"""
    result = java_client.search_books(keyword, max(limit, 5))
    if isinstance(result, list):
        return _fmt_book_list(result, limit)
    return str(result)


def get_book_detail(book_id: int) -> str:
    """根据图书ID获取完整详情，含完整简介、出版社、评分、销量等信息。用于判断图书是否匹配用户需求。"""
    result = java_client.get_book_detail(book_id)
    if isinstance(result, dict):
        return _fmt_book_detail(result)
    return str(result)


def get_user_profile(user_id: int) -> str:
    """获取用户的历史行为数据：收藏的图书、搜索关键词、购买记录等，用于个性化推荐。"""
    result = java_client.get_user_profile(user_id)
    if isinstance(result, dict):
        return _fmt_user_profile(result)
    return str(result)


def add_to_cart(user_id: int, book_id: int, quantity: int = 1) -> str:
    """将图书加入用户的购物车。返回操作结果。"""
    return str(java_client.add_to_cart(user_id, book_id, quantity))


def web_search(query: str) -> str:
    """搜索网页获取图书外部评价、读者感受、图书推荐榜单、适合场景等信息。用于辅助判断图书是否匹配用户意图。"""
    return search_web(query)


def fetch_book_review(url: str) -> str:
    """抓取指定网页内容，用于阅读详细书评或图书介绍。"""
    return fetch_url(url)


TOOLS = [
    StructuredTool.from_function(search_books),
    StructuredTool.from_function(get_book_detail),
    StructuredTool.from_function(get_user_profile),
    StructuredTool.from_function(add_to_cart),
    StructuredTool.from_function(web_search),
    StructuredTool.from_function(fetch_book_review),
]
