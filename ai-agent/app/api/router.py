import os
import re
import logging

from fastapi import APIRouter, HTTPException

from app.agent.core import get_agent_executor
from app.agent.memory import build_memory
from app.agent.prompts import CHAT_SYSTEM_PROMPT, SEARCH_SYSTEM_PROMPT, RECOMMEND_SYSTEM_PROMPT
from app.models.schemas import (
    ChatRequest,
    ChatResponse,
    SearchRequest,
    SearchResponse,
    RecommendRequest,
    RecommendResponse,
)

LOG_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "logs")
os.makedirs(LOG_DIR, exist_ok=True)

logger = logging.getLogger(__name__)
_fh = logging.FileHandler(os.path.join(LOG_DIR, "agent-router.log"), encoding="utf-8", mode="a")
_fh.setFormatter(logging.Formatter("%(asctime)s [%(levelname)s] %(name)s: %(message)s"))
logger.addHandler(_fh)

router = APIRouter(prefix="/agent", tags=["agent"])


@router.get("/test-llm")
def test_llm():
    """Test the LLM connection directly. Returns the raw API response info."""
    from app.config import settings
    from openai import OpenAI

    try:
        client = OpenAI(
            api_key=settings.openai_api_key,
            base_url=settings.openai_api_base,
        )
        resp = client.chat.completions.create(
            model=settings.model_name,
            messages=[{"role": "user", "content": "hello"}],
            temperature=0.3,
            max_tokens=50,
        )
        return {
            "status": "ok",
            "model": settings.model_name,
            "base_url": settings.openai_api_base,
            "has_key": bool(settings.openai_api_key),
            "key_prefix": settings.openai_api_key[:8] + "..." if settings.openai_api_key else "none",
            "choices_count": len(resp.choices) if resp and resp.choices else 0,
            "response_sample": resp.choices[0].message.content if resp and resp.choices else "N/A",
        }
    except Exception as e:
        import traceback
        return {
            "status": "error",
            "error": str(e),
            "traceback": traceback.format_exc(),
            "model": settings.model_name,
            "base_url": settings.openai_api_base,
            "has_key": bool(settings.openai_api_key),
            "key_prefix": settings.openai_api_key[:8] + "..." if settings.openai_api_key else "none",
        }


@router.post("/chat", response_model=ChatResponse)
def chat(req: ChatRequest):
    try:
        executor = get_agent_executor(CHAT_SYSTEM_PROMPT)
        mem, redis_mem = build_memory(str(req.session_id or req.user_id), req.history)

        history_text = ""
        if mem.chat_memory.messages:
            history_text = "历史对话:\n" + "\n".join(
                f"{'用户' if m.type == 'human' else '助手'}: {m.content}"
                for m in mem.chat_memory.messages[-6:]
            ) + "\n\n"

        context = ""
        if req.book_candidates:
            context = "候选图书:\n" + "\n".join(
                f"ID:{b.get('id')} 《{b.get('title')}》"
                f" 作者:{b.get('author')} 价格:¥{b.get('price')}"
                for b in req.book_candidates
            ) + "\n\n"

        input_text = f"{context}{history_text}用户问题: {req.message}"

        result = executor.invoke({"input": input_text})
        reply = result.get("output", "")

        redis_mem.save("user", req.message)
        redis_mem.save("assistant", reply)

        ref_ids = []
        titles = re.findall(r"《(.*?)》", reply)
        for b in req.book_candidates:
            if b.get("title") in titles:
                ref_ids.append(b.get("id"))

        return ChatResponse(reply=reply, referenced_book_ids=ref_ids)
    except Exception as e:
        import traceback
        tb = traceback.format_exc()
        logger.error("Chat endpoint error: %s\n%s", e, tb)
        print(f"=== CHAT ENDPOINT ERROR ===\n{e}\n{tb}", flush=True)
        raise HTTPException(status_code=503, detail=str(e))


def _build_search_input(req: SearchRequest) -> str:
    """Build the input text for the ReAct search agent."""
    parts = []

    # User context (if available)
    if req.user_profile:
        parts.append(f"当前用户画像: {req.user_profile}")
        parts.append(f"用户ID: {req.user_id}（如需更多信息可用 get_user_profile 查询）")
    elif req.user_id:
        parts.append(f"当前用户ID: {req.user_id}，请先调用 get_user_profile 获取用户偏好")

    if req.candidates:
        lines = []
        for c in req.candidates:
            desc = (c.get('description') or '')[:200]
            lines.append(
                f"ID:{c.get('id')} 《{c.get('title')}》"
                f" 作者:{c.get('author')} 简介:{desc}"
            )
        parts.append("初始候选图书（来自关键词粗筛，可能不准确，仅供参考）:\n" + "\n".join(lines))

    parts.append(f"用户搜索意图: {req.keyword}")
    parts.append("""
规则:
1. 先基于候选列表初筛——你对这些书名和作者已经很了解了
2. 对关键候选用 get_book_detail 确认，但不要再三重复查同一本
3. 候选明显不足时才用 search_books 补，不要撒网
4. 不要调用 web_search
5. Final Answer 必须是JSON: {"book_ids":[28,48,38],"reasons":["《长安的荔枝》- 说明","《边城》- 说明"]}""")
    return "\n".join(parts)


def _build_recommend_input(req: RecommendRequest) -> str:
    """Build the input text for the ReAct recommend agent."""
    parts = []
    if req.candidates:
        lines = []
        for c in req.candidates:
            desc = (c.get('description') or '')[:200]
            lines.append(
                f"ID:{c.get('id')} 《{c.get('title')}》"
                f" 作者:{c.get('author')} 价格:¥{c.get('price')} 简介:{desc}"
            )
        parts.append("候选图书:\n" + "\n".join(lines))

    parts.append(f"用户画像: {req.user_profile or '新用户，暂无偏好记录'}")
    parts.append(f"需要推荐 {req.limit} 本图书。")
    parts.append("""
规则:
1. 你可以多次调用工具，但简介为空时不要反复查——根据书名和作者你就能判断风格
2. 不要调用 web_search
3. Final Answer 必须是JSON: {"book_ids":[28,48,38],"reasons":["《长安的荔枝》- 说明","《边城》- 说明"]}""")
    return "\n".join(parts)


def _log_agent_steps(intermediate_steps: list, prefix: str = "") -> None:
    """Log the agent's Thought → Action → Observation chain for debugging."""
    if not intermediate_steps:
        return
    tag = f"[{prefix}] " if prefix else ""
    for i, (action, obs) in enumerate(intermediate_steps, 1):
        logger.info("%sStep %d ───────────────────────", tag, i)
        if hasattr(action, 'log') and action.log:
            log_text = str(action.log).strip()
            # Split by known markers: Observation ends previous step, then Thought/Action follow
            # Remove the trailing Observation if present (it's from previous step)
            obs_marker = "Observation:"
            if obs_marker in log_text:
                log_text = log_text.split(obs_marker)[-1]
            # Print each meaningful line
            for line in log_text.split("\n"):
                line = line.strip()
                if not line:
                    continue
                prefix_lower = line.lower()
                if any(prefix_lower.startswith(kw) for kw in
                       ["thought:", "action:", "action input:", "final answer:"]):
                    logger.info("%s  %s", tag, line)
        # Tool result (truncate long output)
        obs_str = str(obs)
        if len(obs_str) > 600:
            obs_str = obs_str[:600] + "..."
        logger.info("%s  → 结果: %s", tag, obs_str.strip())


def _extract_ids_from_steps(intermediate_steps: list, limit: int = 20) -> list[int]:
    """Fallback: extract book IDs from the books the agent queried via get_book_detail."""
    ids = []
    for action, obs in (intermediate_steps or []):
        ti = action.tool_input
        if isinstance(ti, dict):
            bid = ti.get("book_id")
            if isinstance(bid, (int, float)) and bid > 0:
                ids.append(int(bid))
    seen = set()
    uniq = []
    for i in ids:
        if i not in seen:
            seen.add(i)
            uniq.append(i)
    return uniq[:limit]


def _extract_ids(text: str, limit: int = 20) -> list[int]:
    """Extract book IDs from agent's final answer text."""
    ids = [int(x) for x in re.findall(r"\d+", text)]
    return ids[:limit]


import json as _json

def _parse_final_answer(output_text: str) -> dict:
    """Try to parse JSON from Agent's Final Answer text. Fallback to ID extraction."""
    if not output_text:
        return {"book_ids": [], "reasons": []}
    # Try to find JSON block in the output
    match = re.search(r'\{[^{}]*"book_ids"\s*:\s*\[.*?\][^{}]*\}', output_text, re.DOTALL)
    if match:
        try:
            data = _json.loads(match.group())
            return {"book_ids": data.get("book_ids", []), "reasons": data.get("reasons", [])}
        except Exception:
            pass
    # Fallback: extract bare IDs from text
    ids = [int(x) for x in re.findall(r"\d+", output_text)]
    return {"book_ids": ids[:20], "reasons": []}


@router.post("/search", response_model=SearchResponse)
def search(req: SearchRequest):
    logger.info("=== AGENT SEARCH CALLED ===")
    logger.info("Search request: keyword=%r, candidates_count=%d",
                 req.keyword, len(req.candidates or []))

    try:
        input_text = _build_search_input(req)
        executor = get_agent_executor(SEARCH_SYSTEM_PROMPT)
        result = executor.invoke({"input": input_text})
        output_text = result.get("output", "")

        steps = result.get("intermediate_steps", [])
        _log_agent_steps(steps, "SEARCH")

        logger.info("Agent final output:\n%s", output_text)

        parsed = _parse_final_answer(output_text)
        ids = parsed["book_ids"]
        if not ids:
            ids = _extract_ids_from_steps(steps)

        reasons = parsed.get("reasons", [])
        logger.info("Agent search: %d books, %d reasons", len(ids), len(reasons))
        return SearchResponse(book_ids=ids, reasons=reasons)
    except Exception as e:
        import traceback
        tb = traceback.format_exc()
        logger.error("Search endpoint error: %s\n%s", e, tb)
        raise HTTPException(status_code=503, detail=str(e))


@router.post("/recommend", response_model=RecommendResponse)
def recommend(req: RecommendRequest):
    logger.info("=== AGENT RECOMMEND CALLED ===")

    try:
        input_text = _build_recommend_input(req)
        executor = get_agent_executor(RECOMMEND_SYSTEM_PROMPT)
        result = executor.invoke({"input": input_text})
        output_text = result.get("output", "")

        steps = result.get("intermediate_steps", [])
        _log_agent_steps(steps, "RECOMMEND")

        logger.info("Agent final output:\n%s", output_text)

        parsed = _parse_final_answer(output_text)
        ids = parsed["book_ids"]
        if not ids:
            ids = _extract_ids_from_steps(steps, req.limit)

        reasons = parsed.get("reasons", [])
        logger.info("Agent recommend: %d books, %d reasons", len(ids), len(reasons))
        return RecommendResponse(book_ids=ids, reasons=reasons)
    except Exception as e:
        import traceback
        tb = traceback.format_exc()
        logger.error("Recommend endpoint error: %s\n%s", e, tb)
        raise HTTPException(status_code=503, detail=str(e))
