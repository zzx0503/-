from pydantic import BaseModel
from typing import List, Optional


class ChatRequest(BaseModel):
    user_id: int
    session_id: Optional[int] = None
    message: str
    history: List[dict] = []
    book_candidates: List[dict] = []


class ChatResponse(BaseModel):
    reply: str
    referenced_book_ids: List[int] = []


class SearchRequest(BaseModel):
    keyword: str
    candidates: List[dict] = []
    user_id: Optional[int] = None
    user_profile: str = ""


class SearchResponse(BaseModel):
    book_ids: List[int]
    reasons: List[str] = []


class RecommendRequest(BaseModel):
    user_id: int
    user_profile: str = ""
    candidates: List[dict] = []
    limit: int = 10


class RecommendResponse(BaseModel):
    book_ids: List[int]
    reasons: List[str] = []
