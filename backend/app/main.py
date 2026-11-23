from fastapi import FastAPI

app = FastAPI(title="Voice Notes API")


@app.get("/health")
def health():
    """Проверка, что сервер жив."""
    return {"status": "ok"}
