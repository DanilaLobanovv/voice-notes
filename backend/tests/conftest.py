"""Общие фикстуры для тестов.

Тесты работают с отдельной базой <имя_рабочей_БД>_test, рабочую БД не трогают.
Аудиофайлы пишутся во временную папку pytest (tmp_path), а не в Docker-том.
"""
import pytest
from fastapi.testclient import TestClient
from sqlalchemy import create_engine, text
from sqlalchemy.engine import make_url
from sqlalchemy.orm import sessionmaker

from app import models  # noqa: F401  (регистрирует таблицы в Base.metadata)
from app.config import settings
from app.database import Base, get_db
from app.main import app

# Адрес тестовой БД: тот же сервер, имя с суффиксом _test
WORK_URL = make_url(settings.DATABASE_URL)
TEST_DB_NAME = f"{WORK_URL.database}_test"
TEST_URL = WORK_URL.set(database=TEST_DB_NAME)


def create_test_database():
    """Создаёт тестовую БД, если её ещё нет (через служебную БД postgres)."""
    admin_engine = create_engine(WORK_URL.set(database="postgres"), isolation_level="AUTOCOMMIT")
    with admin_engine.connect() as conn:
        exists = conn.scalar(text("SELECT 1 FROM pg_database WHERE datname = :name"), {"name": TEST_DB_NAME})
        if not exists:
            conn.execute(text(f'CREATE DATABASE "{TEST_DB_NAME}"'))
    admin_engine.dispose()


@pytest.fixture(scope="session")
def test_engine():
    create_test_database()
    engine = create_engine(TEST_URL)
    Base.metadata.create_all(engine)
    yield engine
    engine.dispose()


@pytest.fixture
def client(test_engine, tmp_path, monkeypatch):
    """TestClient, у которого БД — тестовая, а папка аудио — временная."""
    TestSession = sessionmaker(bind=test_engine, autoflush=False)

    def override_get_db():
        db = TestSession()
        try:
            yield db
        finally:
            db.close()

    app.dependency_overrides[get_db] = override_get_db
    monkeypatch.setattr(settings, "AUDIO_DIR", str(tmp_path / "audio"))

    with TestClient(app) as test_client:
        yield test_client

    app.dependency_overrides.clear()
    # Очищаем таблицы, чтобы тесты не влияли друг на друга
    with test_engine.begin() as conn:
        conn.execute(text("TRUNCATE users, notes, audio_records RESTART IDENTITY CASCADE"))


def register_and_login(client, email, password="password123"):
    """Регистрирует пользователя и возвращает заголовок Authorization."""
    response = client.post("/auth/register", json={"email": email, "password": password})
    assert response.status_code == 201
    response = client.post("/auth/login", data={"username": email, "password": password})
    assert response.status_code == 200
    return {"Authorization": f"Bearer {response.json()['access_token']}"}


@pytest.fixture
def auth_headers(client):
    """Зарегистрированный пользователь: заголовки с его токеном."""
    return register_and_login(client, "alice@example.com")


@pytest.fixture
def other_headers(client):
    """Второй пользователь — для проверки изоляции данных."""
    return register_and_login(client, "bob@example.com")


@pytest.fixture
def note(client, auth_headers):
    """Заметка первого пользователя."""
    response = client.post("/notes", json={"title": "Shopping", "text": "milk"}, headers=auth_headers)
    assert response.status_code == 201
    return response.json()


def upload_audio(client, note_id, headers, content=b"fake m4a bytes" * 100, filename="voice.m4a", duration=5):
    """Загружает аудио к заметке и возвращает ответ сервера."""
    return client.post(
        f"/notes/{note_id}/audio",
        files={"file": (filename, content, "audio/mp4")},
        data={"duration_sec": str(duration)},
        headers=headers,
    )
