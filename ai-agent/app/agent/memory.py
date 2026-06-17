import json
import logging

import redis
from langchain.memory import ConversationBufferWindowMemory
from langchain_core.messages import HumanMessage, AIMessage

from app.config import settings

logger = logging.getLogger(__name__)

_redis = redis.Redis(
    host=settings.redis_host,
    port=settings.redis_port,
    password=settings.redis_password or None,
    db=settings.redis_db,
    decode_responses=True
)


def _to_message(msg: dict):
    """Convert a {"role": ..., "content": ...} dict to a LangChain message."""
    role = msg.get("role", "user")
    content = msg.get("content", "")
    if role == "assistant" or role == "ai":
        return AIMessage(content=content)
    return HumanMessage(content=content)


class RedisChatMemory:
    def __init__(self, session_id: str, k: int = 5):
        self.key = f"agent:memory:{session_id}"
        self.k = k

    def load(self) -> list:
        try:
            raw = _redis.lrange(self.key, -self.k, -1)
            return [json.loads(r) for r in raw]
        except Exception as e:
            logger.warning("Redis load failed: %s", e)
            return []

    def save(self, role: str, content: str) -> None:
        try:
            _redis.rpush(self.key, json.dumps({"role": role, "content": content}))
            _redis.ltrim(self.key, -self.k, -1)
            _redis.expire(self.key, 86400 * 7)
        except Exception as e:
            logger.warning("Redis save failed: %s", e)


def build_memory(session_id: str, history: list, k: int = 5):
    """Build a conversation memory from Redis + incoming history."""
    mem = ConversationBufferWindowMemory(k=k, return_messages=True)
    redis_mem = RedisChatMemory(session_id, k)
    for h in redis_mem.load():
        mem.chat_memory.add_message(_to_message(h))
    for h in history:
        mem.chat_memory.add_message(_to_message(h))
    return mem, redis_mem
