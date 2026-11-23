from fastapi import FastAPI

from app.routers import auth

app = FastAPI(title="Voice Notes API")
app.include_router(auth.router)


@app.get("/health")
def health():
    """Проверка, что сервер жив."""
    return {"status": "ok"}
