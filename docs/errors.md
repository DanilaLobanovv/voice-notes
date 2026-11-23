# Ошибки и решения

Формат: ошибка → причина → решение.

- `docker compose` не подключается к демону (`docker.sock: no such file`) → Docker Desktop не был запущен → запустили Docker Desktop.
- `docker-compose.yml` не запускался → внутри `services:` был вложен второй ключ `services:`, а `volumes:` тоже оказался внутри → переписали файл, `services` и `volumes` на верхнем уровне.
- В `requirements.txt` не было FastAPI, SQLAlchemy и т.д. → туда попал `pip freeze` из другого окружения (jupyter, pandas) → заменили на список прямых зависимостей проекта.
- Контейнер `api` не стартовал бы: команда запуска вызывает `alembic upgrade head`, а Alembic не был настроен → выполнили `alembic init` внутри контейнера и взяли URL базы из `DATABASE_URL` в `env.py`.
