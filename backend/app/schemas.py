import uuid
from datetime import datetime

from pydantic import BaseModel, ConfigDict, EmailStr, Field, field_validator


# ---------- Пользователи ----------

class UserCreate(BaseModel):
    email: EmailStr
    password: str = Field(min_length=8)

    @field_validator("password")
    @classmethod
    def password_max_bytes(cls, value: str) -> str:
        # bcrypt принимает не больше 72 байт; кириллица занимает 2 байта на символ
        if len(value.encode()) > 72:
            raise ValueError("Password must be at most 72 bytes")
        return value

    @field_validator("email")
    @classmethod
    def lower_email(cls, value: str) -> str:
        return value.lower()


class UserOut(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    email: str
    created_at: datetime


class Token(BaseModel):
    access_token: str
    token_type: str = "bearer"


# ---------- Аудиозаписи ----------

class AudioOut(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: uuid.UUID
    note_id: uuid.UUID
    duration_sec: float
    size_bytes: int
    created_at: datetime


# ---------- Заметки ----------

class NoteCreate(BaseModel):
    # Обрезаем пробелы по краям, чтобы заголовок "   " считался пустым (422)
    model_config = ConfigDict(str_strip_whitespace=True)

    title: str = Field(min_length=1, max_length=120)
    text: str | None = None


class NoteUpdate(BaseModel):
    model_config = ConfigDict(str_strip_whitespace=True)

    # Оба поля необязательны: меняем только то, что прислали
    title: str | None = Field(default=None, min_length=1, max_length=120)
    text: str | None = None


class NoteOut(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: uuid.UUID
    title: str
    text: str | None
    created_at: datetime
    updated_at: datetime


class NoteDetail(NoteOut):
    audio_records: list[AudioOut]
