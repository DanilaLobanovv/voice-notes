# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project state

Early scaffold of a "voice notes" backend. All modules under `backend/app/` are currently **empty files**. The layout shows the intended design; nothing is implemented yet. There is no Alembic setup, no tests and no lint config.

## Running

The intended runtime is Docker Compose: a Postgres 16 `db` service plus an `api` service built from `backend/`.

```sh
docker compose up --build        # needs a .env at repo root with POSTGRES_USER / POSTGRES_PASSWORD / POSTGRES_DB
```

On start, the API container runs `alembic upgrade head && uvicorn app.main:app --host 0.0.0.0 --port 8000`, so:
- `app.main` must expose a FastAPI `app`.
- An Alembic config (`backend/alembic.ini` and a migrations dir) must exist before the container can boot.
- `./backend` is bind-mounted to `/app`, so migrations generated inside the container (for example `docker compose exec api alembic revision --autogenerate -m "..."`) appear on the host.
- `DATABASE_URL` is injected as `postgresql+psycopg2://...@db:5432/...`, which means SQLAlchemy with the psycopg2 driver.
- Uploaded audio goes in the `audio` volume, mounted at `/data/audio`.

## Known scaffold issues (fix before expecting things to run)

- `docker-compose.yml` nests a second `services:` key under the top-level `services:`, and `volumes:` is indented inside it too. Both must sit at the top level.
- `backend/requirements.txt` is an unrelated pip freeze (jupyter, pandas, matplotlib, …). It has none of the packages the app needs (fastapi, uvicorn, sqlalchemy, psycopg2-binary, alembic, pydantic-settings, auth/password-hashing libs, python-multipart for audio uploads).
- The root `pyproject.toml`/`uv.lock` is a default `uv init --package` stub (`voice_notes:main` entry point, no `src/voice_notes` package, no dependencies). The Docker build ignores it and only uses `backend/requirements.txt`. Pick one dependency source of truth.

## Intended architecture (`backend/app/`)

Standard FastAPI layering:
- `config.py`: settings from env (`DATABASE_URL`, secrets, audio storage path).
- `database.py`: SQLAlchemy engine, session factory and declarative `Base`.
- `models.py` / `schemas.py`: ORM models and Pydantic request/response schemas.
- `security.py`: password hashing and token creation/verification.
- `deps.py`: FastAPI dependencies (DB session, current authenticated user).
- `routers/`: `auth` (register/login), `notes` (note CRUD), `audio` (upload and serve audio files stored under `/data/audio`). These are wired into `main.py`.
