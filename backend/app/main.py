from fastapi import FastAPI

from app.routers import audio, auth, notes

app = FastAPI(title="Voice Notes API")
app.include_router(auth.router)
app.include_router(notes.router)
app.include_router(audio.router)


@app.get("/health")
def health():
    """Проверка, что сервер жив."""
    return {"status": "ok"}
