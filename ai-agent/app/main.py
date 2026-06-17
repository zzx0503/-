import logging
import os
import sys

import uvicorn
from fastapi import FastAPI

from app.api.router import router
from app.config import settings

LOG_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "logs")
os.makedirs(LOG_DIR, exist_ok=True)

logging.basicConfig(
    level=getattr(logging, settings.log_level.upper(), logging.INFO),
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
    datefmt="%H:%M:%S",
    handlers=[
        logging.StreamHandler(sys.stdout),
        logging.FileHandler(os.path.join(LOG_DIR, "agent-debug.log"), encoding="utf-8"),
    ],
)

app = FastAPI(title="Bookstore AI Agent", version="1.0.0")
app.include_router(router)


@app.get("/health")
def health():
    return {"status": "ok"}

if __name__ == "__main__":
    uvicorn.run(
        "app.main:app",
        host="0.0.0.0",
        port=settings.port,
        reload=True,
        reload_dirs=[os.path.join(os.path.dirname(__file__), d) for d in
                     ["agent", "api", "client", "models"]],
    )
