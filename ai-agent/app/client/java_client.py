import httpx
from app.config import settings

_client = httpx.Client(base_url=settings.java_base_url, timeout=30.0)


def _call(tool: str, payload: dict) -> dict:
    headers = {"X-API-Key": settings.java_api_key, "Content-Type": "application/json"}
    resp = _client.post(f"/internal/agent-tools/{tool}", json=payload, headers=headers)
    resp.raise_for_status()
    data = resp.json()
    if data.get("code") != 200:
        raise RuntimeError(f"Java tool error: {data.get('msg')}")
    return data.get("data", {})


def search_books(keyword: str, limit: int = 10) -> list:
    return _call("search-books", {"keyword": keyword, "limit": limit})


def get_book_detail(book_id: int) -> dict:
    return _call("book-detail", {"book_id": book_id})


def get_user_profile(user_id: int) -> dict:
    return _call("user-profile", {"user_id": user_id})


def add_to_cart(user_id: int, book_id: int, quantity: int = 1) -> dict:
    return _call("add-to-cart", {"user_id": user_id, "book_id": book_id, "quantity": quantity})
