from fastapi import FastAPI, Request
from fastapi.responses import JSONResponse

from app.routers import audio, auth, notes

# 20 МБ файла + запас на служебные части form-data (имя поля, duration_sec и т.п.)
MAX_REQUEST_BYTES = audio.MAX_SIZE_BYTES + 64 * 1024

app = FastAPI(title="Voice Notes API")


@app.middleware("http")
async def limit_request_size(request: Request, call_next):
    """Отклоняем слишком большой запрос сразу по заголовку Content-Length,
    не дожидаясь, пока сервер примет весь файл во временную папку."""
    content_length = request.headers.get("content-length")
    if content_length and content_length.isdigit() and int(content_length) > MAX_REQUEST_BYTES:
        return JSONResponse(status_code=413, content={"detail": "Request is too large"})
    return await call_next(request)
app.include_router(auth.router)
app.include_router(notes.router)
app.include_router(audio.router)


@app.get("/health")
def health():
    """Проверка, что сервер жив."""
    return {"status": "ok"}
