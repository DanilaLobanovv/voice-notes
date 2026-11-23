import os
import uuid

from fastapi import APIRouter, Depends, File, Form, HTTPException, UploadFile, status
from fastapi.responses import FileResponse
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.config import settings
from app.database import get_db
from app.deps import get_current_user
from app.models import AudioRecord, Note, User
from app.routers.notes import get_own_note
from app.schemas import AudioOut
from app.storage import delete_file

router = APIRouter(tags=["audio"])

MAX_SIZE_BYTES = 20 * 1024 * 1024  # 20 МБ
MAX_DURATION_SEC = 600
CHUNK_SIZE = 1024 * 1024  # читаем файл кусками по 1 МБ


def get_own_audio(audio_id: uuid.UUID, db: Session, user: User) -> AudioRecord:
    """Запись ищем через JOIN с заметкой: она должна принадлежать текущему пользователю."""
    record = db.scalar(
        select(AudioRecord)
        .join(Note, AudioRecord.note_id == Note.id)
        .where(AudioRecord.id == audio_id, Note.user_id == user.id)
    )
    if record is None:
        raise HTTPException(status.HTTP_404_NOT_FOUND, "Audio not found")
    return record


@router.post("/notes/{note_id}/audio", response_model=AudioOut, status_code=status.HTTP_201_CREATED)
def upload_audio(
    note_id: uuid.UUID,
    file: UploadFile = File(...),
    duration_sec: float = Form(..., gt=0, le=MAX_DURATION_SEC),
    db: Session = Depends(get_db),
    user: User = Depends(get_current_user),
):
    note = get_own_note(note_id, db, user)

    if not (file.filename or "").lower().endswith(".m4a"):
        raise HTTPException(status.HTTP_415_UNSUPPORTED_MEDIA_TYPE, "Only .m4a files are allowed")

    # Имя файла на диске придумываем сами (id записи), имя от клиента не используем —
    # так нельзя подсунуть путь вроде "../../etc/passwd"
    audio_id = uuid.uuid4()
    note_dir = os.path.join(settings.AUDIO_DIR, str(note.id))
    os.makedirs(note_dir, exist_ok=True)
    path = os.path.join(note_dir, f"{audio_id}.m4a")

    # Пишем кусками и считаем размер; как только превысили лимит — удаляем и отвечаем 413
    size = 0
    with open(path, "wb") as out:
        while chunk := file.file.read(CHUNK_SIZE):
            size += len(chunk)
            if size > MAX_SIZE_BYTES:
                break
            out.write(chunk)
    if size > MAX_SIZE_BYTES:
        delete_file(path)
        raise HTTPException(status.HTTP_413_REQUEST_ENTITY_TOO_LARGE, "File is larger than 20 MB")

    record = AudioRecord(id=audio_id, note_id=note.id, file_path=path, duration_sec=duration_sec, size_bytes=size)
    db.add(record)
    db.commit()
    db.refresh(record)
    return record


@router.get("/audio/{audio_id}", response_class=FileResponse)
def download_audio(audio_id: uuid.UUID, db: Session = Depends(get_db), user: User = Depends(get_current_user)):
    record = get_own_audio(audio_id, db, user)
    if not os.path.exists(record.file_path):
        raise HTTPException(status.HTTP_404_NOT_FOUND, "Audio file is missing")
    return FileResponse(record.file_path, media_type="audio/mp4", filename=f"{record.id}.m4a")


@router.delete("/audio/{audio_id}", status_code=status.HTTP_204_NO_CONTENT)
def delete_audio(audio_id: uuid.UUID, db: Session = Depends(get_db), user: User = Depends(get_current_user)):
    record = get_own_audio(audio_id, db, user)
    path = record.file_path
    db.delete(record)
    db.commit()
    delete_file(path)
