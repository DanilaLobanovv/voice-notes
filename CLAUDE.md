# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

`README.md` is the spec (written in Russian): domain rules (§4), DB schema (§5), API and error codes (§6), mandatory technical decisions and git/agent rules (§8), and the step plan (§9). Read it before changing behaviour. The author is a beginner, so keep code simple and add short Russian comments where logic isn't obvious.

## Commands

Everything runs in Docker from the repo root (needs `.env`, copied from `.env.example`):

```sh
docker compose up --build -d                     # api runs `alembic upgrade head` then uvicorn on :8000
docker compose restart api                       # uvicorn has no --reload; restart after code edits
docker compose run --rm api pytest -q            # full test suite
docker compose run --rm api pytest -q tests/test_notes.py::test_name   # single test
docker compose run --rm api alembic revision --autogenerate -m "msg"   # after editing models.py
```

`./backend` is bind-mounted at `/app`, so generated migrations and test files show up on the host. Tests use a separate `<POSTGRES_DB>_test` database and a tmp `AUDIO_DIR`, and never touch working data. Never run `docker compose down -v` without the author's permission: it deletes the `pgdata` and `audio` volumes.

## Architecture (`backend/app/`)

- `config.py`: `settings` (pydantic-settings) from env: `DATABASE_URL`, `SECRET_KEY`, `ACCESS_TOKEN_EXPIRE_MINUTES`, `AUDIO_DIR`. Code reads `settings.AUDIO_DIR` at request time, so tests can monkeypatch it.
- `database.py`: engine, `SessionLocal`, `Base`, and the `get_db` dependency (tests override it).
- `models.py`: `User` (int id) → `Note` (UUID) → `AudioRecord` (UUID). Every FK has `ondelete="CASCADE"` and every relationship has `cascade="all, delete-orphan"`. `alembic/env.py` imports `models` and takes its URL from `settings`.
- `security.py`: bcrypt used directly (no passlib) and PyJWT HS256 with `sub` = user id. `deps.get_current_user` uses `OAuth2PasswordBearer(tokenUrl="/auth/login")`; login takes an OAuth2 form where `username` is the email.
- **Ownership rule:** `routers/notes.get_own_note` filters by `Note.user_id == user.id` and `routers/audio.get_own_audio` JOINs to `Note` on the owner. Another user's object must be 404, never 403. Reuse these helpers for any new endpoint.
- Audio files are stored at `AUDIO_DIR/<note_id>/<audio_id>.m4a`. The client filename is never used for the path. The DB only stores the path and metadata. On delete, the DB row is removed first, then `storage.delete_file`. Limits: `.m4a` only (415), ≤20 MB checked while streaming (413), `0 < duration_sec ≤ 600` (422).

## Workflow rules (from README §8)

- Git flow: `main` ← `develop` ← `feature/*`. The author merges into `develop` after peer review. Never `git push`, open PRs or change remotes without explicit permission.
- Log every failed check as one line "ошибка → причина → решение" in `docs/errors.md`. It's material for `docs/report-stage2.md`.
- Never commit `.env` or audio files, and never print secrets or tokens.
- Project subagents: `.claude/agents/code-reviewer.md` (read-only review against the README) and `.claude/agents/test-engineer.md` (writes and runs pytest).
