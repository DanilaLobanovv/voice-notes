# Запуск Voice Notes API

Нужно: Docker Desktop (запущенный) и Git.

```bash
git clone <адрес репозитория> voice-notes && cd voice-notes
cp .env.example .env
# в .env заменить POSTGRES_PASSWORD и SECRET_KEY; ключ сгенерировать так:
openssl rand -hex 32
docker compose up --build -d
```

При старте контейнер `api` сам применяет миграции (`alembic upgrade head`) и запускает сервер.

Проверить:
- `docker compose ps`: `db` в статусе healthy, `api` в статусе running;
- `curl localhost:8000/health` возвращает `{"status":"ok"}`;
- документация API: http://localhost:8000/docs.

Полезные команды:

| Что | Команда |
|---|---|
| логи сервера | `docker compose logs -f api` |
| автотесты | `docker compose run --rm api pytest -q` |
| новая миграция после правки `models.py` | `docker compose run --rm api alembic revision --autogenerate -m "описание"` |
| применить миграции | `docker compose run --rm api alembic upgrade head` |
| консоль БД | `docker compose exec db psql -U <POSTGRES_USER> -d <POSTGRES_DB>` |
| перезапустить сервер после правки кода | `docker compose restart api` |
| остановить (данные сохраняются) | `docker compose down` |
| остановить и **удалить все данные** | `docker compose down -v` |
