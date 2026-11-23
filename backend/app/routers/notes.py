import os
import shutil
import uuid

from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.config import settings
from app.database import get_db
from app.deps import get_current_user
from app.models import Note, User
from app.schemas import NoteCreate, NoteDetail, NoteOut, NoteUpdate
from app.storage import delete_file

router = APIRouter(prefix="/notes", tags=["notes"])


def get_own_note(note_id: uuid.UUID, db: Session, user: User) -> Note:
    """Ищем заметку только среди заметок текущего пользователя.
    Чужая и несуществующая заметка выглядят одинаково — 404."""
    note = db.scalar(select(Note).where(Note.id == note_id, Note.user_id == user.id))
    if note is None:
        raise HTTPException(status.HTTP_404_NOT_FOUND, "Note not found")
    return note


@router.get("", response_model=list[NoteOut])
def list_notes(db: Session = Depends(get_db), user: User = Depends(get_current_user)):
    return db.scalars(
        select(Note).where(Note.user_id == user.id).order_by(Note.updated_at.desc())
    ).all()


@router.post("", response_model=NoteOut, status_code=status.HTTP_201_CREATED)
def create_note(data: NoteCreate, db: Session = Depends(get_db), user: User = Depends(get_current_user)):
    note = Note(user_id=user.id, title=data.title, text=data.text)
    db.add(note)
    db.commit()
    db.refresh(note)
    return note


@router.get("/{note_id}", response_model=NoteDetail)
def get_note(note_id: uuid.UUID, db: Session = Depends(get_db), user: User = Depends(get_current_user)):
    return get_own_note(note_id, db, user)


@router.put("/{note_id}", response_model=NoteOut)
def update_note(
    note_id: uuid.UUID,
    data: NoteUpdate,
    db: Session = Depends(get_db),
    user: User = Depends(get_current_user),
):
    note = get_own_note(note_id, db, user)
    # exclude_unset: берём только поля, которые клиент реально прислал
    changes = data.model_dump(exclude_unset=True)
    if "title" in changes and changes["title"] is None:
        raise HTTPException(status.HTTP_422_UNPROCESSABLE_CONTENT, "Title cannot be null")
    for field, value in changes.items():
        setattr(note, field, value)
    db.commit()
    db.refresh(note)
    return note


@router.delete("/{note_id}", status_code=status.HTTP_204_NO_CONTENT)
def delete_note(note_id: uuid.UUID, db: Session = Depends(get_db), user: User = Depends(get_current_user)):
    note = get_own_note(note_id, db, user)
    paths = [record.file_path for record in note.audio_records]
    # Сначала удаляем строки из БД (аудио удалятся каскадом), потом файлы с диска
    db.delete(note)
    db.commit()
    for path in paths:
        delete_file(path)
    # Удаляем папку заметки целиком (файлы её записей уже удалены выше)
    shutil.rmtree(os.path.join(settings.AUDIO_DIR, str(note_id)), ignore_errors=True)
