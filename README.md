# Voice Notes API

Серверная часть приложения для текстовых заметок с аудиозаписями. REST API на FastAPI и PostgreSQL, запускается одной командой через Docker Compose.

Клиент на Android (Kotlin + Jetpack Compose) будет на следующем этапе и будет работать с этим API.

## Возможности

- регистрация и вход по email и паролю, авторизация по JWT-токену;
- создание, просмотр, изменение и удаление заметок;
- загрузка аудиозаписей M4A к заметке, скачивание и удаление;
- каждый пользователь видит только свои данные: на чужую заметку сервер отвечает 404;
- удаление заметки удаляет и её аудио (и записи в базе, и файлы на диске).

## Технологии

| Что | Чем |
|---|---|
| Язык и фреймворк | Python 3.12, FastAPI |
| База данных | PostgreSQL 16, SQLAlchemy 2, миграции Alembic |
| Вход | JWT (PyJWT, HS256, срок 60 минут), пароли хешируются bcrypt |
| Запуск | Docker Compose: сервисы `api` и `db` |
| Тесты | pytest + TestClient |

## Быстрый старт

Нужен установленный и запущенный [Docker Desktop](https://www.docker.com/products/docker-desktop/).

```bash
git clone https://github.com/DanilaLobanovv/voice-notes.git
cd voice-notes
cp .env.example .env
```

Откройте `.env` и замените значения:
- `POSTGRES_PASSWORD`: любой пароль из букв и цифр, например результат `openssl rand -hex 16`;
- `SECRET_KEY`: результат команды `openssl rand -hex 32`.

Запуск:

```bash
docker compose up --build -d
```

Проверка:

```bash
docker compose ps             # db: healthy, api: running
curl localhost:8000/health    # {"status":"ok"}
```

Документация API с возможностью отправлять запросы: **http://localhost:8000/docs**. Чтобы вызывать защищённые адреса, зарегистрируйтесь через `POST /auth/register`, нажмите **Authorize** и введите email в поле `username` и пароль.

Остановить: `docker compose down` (данные сохраняются). Остановить и удалить все данные: `docker compose down -v`.

## API

| Метод и адрес | Что делает | Успех |
|---|---|---|
| `POST /auth/register` | регистрация (JSON: `email`, `password`) | 201 |
| `POST /auth/login` | вход (форма: `username`=email, `password`), возвращает токен | 200 |
| `GET /auth/me` | текущий пользователь | 200 |
| `GET /notes` | список своих заметок | 200 |
| `POST /notes` | создать заметку (JSON: `title`, `text`) | 201 |
| `GET /notes/{id}` | заметка вместе со списком её аудио | 200 |
| `PUT /notes/{id}` | изменить заголовок и/или текст | 200 |
| `DELETE /notes/{id}` | удалить заметку и её аудио | 204 |
| `POST /notes/{id}/audio` | загрузить аудио (form-data: `file`, `duration_sec`) | 201 |
| `GET /audio/{id}` | скачать аудиофайл | 200 |
| `DELETE /audio/{id}` | удалить аудио | 204 |
| `GET /health` | проверка, что сервер работает | 200 |

Все адреса, кроме `/auth/register`, `/auth/login` и `/health`, требуют заголовок `Authorization: Bearer <токен>`.

Пример через curl:

```bash
curl -X POST localhost:8000/auth/register -H "Content-Type: application/json" \
     -d '{"email": "me@example.com", "password": "password123"}'

TOKEN=$(curl -s -X POST localhost:8000/auth/login -d "username=me@example.com&password=password123" \
        | python3 -c "import json,sys; print(json.load(sys.stdin)['access_token'])")

curl -X POST localhost:8000/notes -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
     -d '{"title": "Первая заметка", "text": "Привет"}'
```

### Правила и ошибки

- Пароль: минимум 8 символов. Email приводится к нижнему регистру и должен быть уникальным.
- Заголовок заметки обязателен, от 1 до 120 символов. Текст необязателен.
- Аудио: только `.m4a`, не больше 20 МБ и не дольше 600 секунд.

| Код | Когда |
|---|---|
| 401 | нет токена, токен неверный или просрочен, неверный email или пароль |
| 404 | объект не существует или принадлежит другому пользователю |
| 409 | email уже зарегистрирован |
| 413 | файл больше 20 МБ |
| 415 | файл не M4A |
| 422 | неверные данные (короткий пароль, пустой заголовок, длительность больше 600 секунд) |

## База данных

```
users ──< notes ──< audio_records
```

- `users`: `id`, `email`, `password_hash`, `created_at`
- `notes`: `id` (UUID), `user_id`, `title`, `text`, `created_at`, `updated_at`
- `audio_records`: `id` (UUID), `note_id`, `file_path`, `duration_sec`, `size_bytes`, `created_at`

У пользователя много заметок, у заметки много аудиозаписей, удаление каскадное. Аудиофайлы лежат в Docker-томе `audio`, в базе хранятся только путь и метаданные. Подробнее: [docs/db-schema.md](docs/db-schema.md).

## Разработка

| Что | Команда |
|---|---|
| автотесты | `docker compose run --rm api pytest -q` |
| один тест | `docker compose run --rm api pytest -q tests/test_notes.py::test_create_note_returns_201` |
| перезапустить сервер после правки кода | `docker compose restart api` |
| новая миграция после правки `models.py` | `docker compose run --rm api alembic revision --autogenerate -m "описание"` |
| логи сервера | `docker compose logs -f api` |
| консоль базы данных | `docker compose exec db psql -U <POSTGRES_USER> -d <POSTGRES_DB>` |

Тесты работают на отдельной базе `<POSTGRES_DB>_test` и временной папке для аудио, поэтому рабочие данные не трогают.

Ветки: `main` (стабильная версия) ← `develop` ← `feature/*`.

## Структура

```
voice-notes/
├── docker-compose.yml       сервисы api и db, тома pgdata и audio
├── .env.example             образец настроек (.env в Git не попадает)
├── docs/                    схема БД, отчёт, журнал ошибок, ТЗ этапа
└── backend/
    ├── Dockerfile
    ├── requirements.txt
    ├── alembic/             миграции базы данных
    ├── tests/               автотесты pytest
    └── app/
        ├── main.py          создание приложения, подключение роутеров
        ├── config.py        настройки из переменных окружения
        ├── database.py      подключение к БД
        ├── models.py        таблицы (SQLAlchemy)
        ├── schemas.py       форматы запросов и ответов (Pydantic)
        ├── security.py      хеширование паролей и JWT
        ├── deps.py          получение текущего пользователя по токену
        ├── storage.py       работа с файлами на диске
        └── routers/         auth.py, notes.py, audio.py
```

## Документы

- [docs/spec-stage2.md](docs/spec-stage2.md): техническое задание этапа 2;
- [docs/db-schema.md](docs/db-schema.md): схема базы данных и код для dbdiagram.io;
- [docs/report-stage2.md](docs/report-stage2.md): отчёт по этапу 2;
- [docs/errors.md](docs/errors.md): ошибки, найденные при разработке, и как их исправили;
- [DEPLOY.md](DEPLOY.md): краткая инструкция запуска.

## Что дальше

Этап 3: Android-клиент на Kotlin и Jetpack Compose. Экраны входа и списка заметок, запись звука через `MediaRecorder`, воспроизведение через Media3 ExoPlayer, Retrofit для работы с этим API, Room для локального кеша.
