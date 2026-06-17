import os
from pydantic_settings import BaseSettings, SettingsConfigDict

_env_file = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", ".env")


class Settings(BaseSettings):
    model_config = SettingsConfigDict(
        env_file=_env_file, env_file_encoding="utf-8",
        env_prefix="AGENT_",
        protected_namespaces=('settings_',),
    )

    openai_api_base: str = "https://dashscope.aliyuncs.com/compatible-mode/v1"
    openai_api_key: str = ""
    model_name: str = "qwen-plus"
    temperature: float = 0.7
    max_tokens: int = 512

    java_base_url: str = "http://localhost:8080"
    java_api_key: str = ""

    redis_host: str = "localhost"
    redis_port: int = 6379
    redis_password: str = ""
    redis_db: int = 0

    port: int = 8000
    log_level: str = "info"


settings = Settings()
