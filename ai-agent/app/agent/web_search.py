"""Web search and web fetch utilities for the agent."""

import re
import httpx
from bs4 import BeautifulSoup

_HEADERS = {
    "User-Agent": (
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
        "AppleWebKit/537.36 (KHTML, like Gecko) "
        "Chrome/125.0.0.0 Safari/537.36"
    )
}
_HTTP_TIMEOUT = 15.0


def search_web(query: str, max_results: int = 5) -> str:
    """Search the web using DuckDuckGo and return text summaries."""
    try:
        from duckduckgo_search import DDGS
        with DDGS() as ddgs:
            results = list(ddgs.text(query, max_results=max_results))
        if not results:
            return "未找到相关结果，请改用 search_books 在书店内搜索。"
        lines = []
        for i, r in enumerate(results, 1):
            title = r.get("title", "")
            body = r.get("body", "")
            href = r.get("href", "")
            lines.append(f"{i}. {title}\n   {body}\n   链接: {href}")
        return "\n\n".join(lines)
    except ImportError:
        return "网络搜索不可用，请改用 search_books 在书店数据库内搜索。"
    except Exception as e:
        return f"网络搜索暂时不可用（{str(e)[:100]}），请不要重试。改用 search_books 和 get_book_detail 在书店内搜索。"


def fetch_url(url: str, max_chars: int = 3000) -> str:
    """Fetch a URL and extract readable text content."""
    try:
        resp = httpx.get(url, headers=_HEADERS, timeout=_HTTP_TIMEOUT, follow_redirects=True)
        resp.raise_for_status()
        soup = BeautifulSoup(resp.text, "lxml")

        # Remove script, style, nav, footer, header
        for tag in soup.find_all(["script", "style", "nav", "footer", "header", "aside"]):
            tag.decompose()

        text = soup.get_text(separator="\n", strip=True)
        text = re.sub(r"\n{3,}", "\n\n", text)

        if len(text) > max_chars:
            text = text[:max_chars] + "..."

        title = soup.title.string.strip() if soup.title and soup.title.string else ""
        result = f"标题: {title}\n\n{text}" if title else text
        return result.strip() or "页面内容为空。"
    except Exception as e:
        return f"获取页面失败: {str(e)}"
