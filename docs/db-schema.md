# Схема базы данных

## Таблицы

### users — пользователи
| Поле | Тип | Описание |
|---|---|---|
| id | integer, PK | номер пользователя |
| email | varchar(255), unique | email в нижнем регистре |
| password_hash | varchar(255) | bcrypt-хеш пароля (сам пароль не хранится) |
| created_at | timestamptz | дата регистрации |

### notes — заметки
| Поле | Тип | Описание |
|---|---|---|
| id | uuid, PK | идентификатор заметки |
| user_id | integer, FK → users.id, ON DELETE CASCADE | владелец |
| title | varchar(120), обязательно | заголовок |
| text | text, может быть пустым | текст заметки |
| created_at | timestamptz | дата создания |
| updated_at | timestamptz | дата последнего изменения |

### audio_records — аудиозаписи
| Поле | Тип | Описание |
|---|---|---|
| id | uuid, PK | идентификатор записи |
| note_id | uuid, FK → notes.id, ON DELETE CASCADE | заметка, к которой относится запись |
| file_path | varchar(500) | путь к файлу `.m4a` в Docker-томе `audio` |
| duration_sec | double precision | длительность, секунды (до 600) |
| size_bytes | integer | размер файла, байты (до 20 МБ) |
| created_at | timestamptz | дата загрузки |

Служебная таблица `alembic_version` хранит номер применённой миграции.

## Связи

- Пользователь → заметки: один ко многим (1:N).
- Заметка → аудиозаписи: один ко многим (1:N).
- Удаление каскадное: удалили пользователя — удалились его заметки; удалили заметку — удалились её аудиозаписи (строки в БД через `ON DELETE CASCADE`, файлы на диске удаляет код API).

## Код для dbdiagram.io

Вставьте на https://dbdiagram.io/d, чтобы получить картинку схемы:

```
Table users {
  id integer [pk, increment]
  email varchar(255) [unique, not null]
  password_hash varchar(255) [not null]
  created_at timestamptz [not null, default: `now()`]
}

Table notes {
  id uuid [pk]
  user_id integer [not null, ref: > users.id]
  title varchar(120) [not null]
  text text
  created_at timestamptz [not null, default: `now()`]
  updated_at timestamptz [not null, default: `now()`]
}

Table audio_records {
  id uuid [pk]
  note_id uuid [not null, ref: > notes.id]
  file_path varchar(500) [not null]
  duration_sec float [not null]
  size_bytes integer [not null]
  created_at timestamptz [not null, default: `now()`]
}
```
