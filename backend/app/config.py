from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    """Настройки берутся из переменных окружения (в Docker — из .env)."""

    DATABASE_URL: str
    SECRET_KEY: str
    ACCESS_TOKEN_EXPIRE_MINUTES: int = 60
    AUDIO_DIR: str = "/data/audio"


settings = Settings()
