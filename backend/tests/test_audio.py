import os

from app.config import settings
from tests.conftest import upload_audio


def files_on_disk():
    """Все файлы, которые лежат во временной папке аудио."""
    found = []
    for root, _dirs, files in os.walk(settings.AUDIO_DIR):
        found += [os.path.join(root, name) for name in files]
    return found


def test_upload_m4a_returns_201(client, auth_headers, note):
    content = b"a" * 3000
    response = upload_audio(client, note["id"], auth_headers, content=content, duration=12.5)
    assert response.status_code == 201
    body = response.json()
    assert body["note_id"] == note["id"]
    assert body["size_bytes"] == 3000
    assert body["duration_sec"] == 12.5
    assert len(files_on_disk()) == 1


def test_uploaded_audio_is_listed_in_note(client, auth_headers, note):
    audio_id = upload_audio(client, note["id"], auth_headers).json()["id"]
    records = client.get(f"/notes/{note['id']}", headers=auth_headers).json()["audio_records"]
    assert [r["id"] for r in records] == [audio_id]


def test_download_returns_same_bytes(client, auth_headers, note):
    content = os.urandom(5000)
    audio_id = upload_audio(client, note["id"], auth_headers, content=content).json()["id"]
    response = client.get(f"/audio/{audio_id}", headers=auth_headers)
    assert response.status_code == 200
    assert response.content == content


def test_uppercase_extension_is_accepted(client, auth_headers, note):
    assert upload_audio(client, note["id"], auth_headers, filename="VOICE.M4A").status_code == 201


def test_mp3_returns_415(client, auth_headers, note):
    response = upload_audio(client, note["id"], auth_headers, filename="song.mp3")
    assert response.status_code == 415
    assert files_on_disk() == []


def test_file_larger_than_20mb_returns_413(client, auth_headers, note):
    content = b"0" * (20 * 1024 * 1024 + 1)
    response = upload_audio(client, note["id"], auth_headers, content=content)
    assert response.status_code == 413
    assert files_on_disk() == []  # недокачанный файл удалён
    assert client.get(f"/notes/{note['id']}", headers=auth_headers).json()["audio_records"] == []


def test_file_of_exactly_20mb_is_accepted(client, auth_headers, note):
    content = b"0" * (20 * 1024 * 1024)
    assert upload_audio(client, note["id"], auth_headers, content=content).status_code == 201


def test_duration_700_returns_422(client, auth_headers, note):
    assert upload_audio(client, note["id"], auth_headers, duration=700).status_code == 422


def test_duration_zero_returns_422(client, auth_headers, note):
    assert upload_audio(client, note["id"], auth_headers, duration=0).status_code == 422


def test_upload_to_missing_note_returns_404(client, auth_headers):
    missing = "00000000-0000-0000-0000-000000000000"
    assert upload_audio(client, missing, auth_headers).status_code == 404


def test_delete_audio_removes_file(client, auth_headers, note):
    audio_id = upload_audio(client, note["id"], auth_headers).json()["id"]
    assert client.delete(f"/audio/{audio_id}", headers=auth_headers).status_code == 204
    assert files_on_disk() == []
    assert client.get(f"/audio/{audio_id}", headers=auth_headers).status_code == 404


def test_delete_note_removes_audio_files(client, auth_headers, note):
    upload_audio(client, note["id"], auth_headers)
    upload_audio(client, note["id"], auth_headers)
    assert len(files_on_disk()) == 2

    assert client.delete(f"/notes/{note['id']}", headers=auth_headers).status_code == 204
    assert files_on_disk() == []
