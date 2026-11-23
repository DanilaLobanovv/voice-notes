from sqlalchemy import create_engine
from sqlalchemy.orm import DeclarativeBase, sessionmaker

from app.config import settings

# pool_pre_ping проверяет соединение перед использованием (БД могла перезапуститься)
engine = create_engine(settings.DATABASE_URL, pool_pre_ping=True)
SessionLocal = sessionmaker(bind=engine, autoflush=False)


class Base(DeclarativeBase):
    pass


def get_db():
    """Зависимость FastAPI: одна сессия БД на запрос, закрывается в конце."""
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()
