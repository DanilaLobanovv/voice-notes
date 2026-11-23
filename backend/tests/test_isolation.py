"""Второй пользователь не должен видеть и менять чужие данные: везде 404."""
from tests.conftest import upload_audio


def test_other_user_does_not_see_note_in_list(client, note, other_headers):
    response = client.get("/notes", headers=other_headers)
    assert response.status_code == 200
    assert response.json() == []


def test_other_user_get_note_returns_404(client, note, other_headers):
    assert client.get(f"/notes/{note['id']}", headers=other_headers).status_code == 404


def test_other_user_update_note_returns_404(client, auth_headers, note, other_headers):
    response = client.put(f"/notes/{note['id']}", json={"title": "Hacked"}, headers=other_headers)
    assert response.status_code == 404
    # и заметка владельца не изменилась
    assert client.get(f"/notes/{note['id']}", headers=auth_headers).json()["title"] == "Shopping"


def test_other_user_delete_note_returns_404(client, auth_headers, note, other_headers):
    assert client.delete(f"/notes/{note['id']}", headers=other_headers).status_code == 404
    assert client.get(f"/notes/{note['id']}", headers=auth_headers).status_code == 200


def test_other_user_upload_audio_to_note_returns_404(client, note, other_headers):
    assert upload_audio(client, note["id"], other_headers).status_code == 404


def test_other_user_download_audio_returns_404(client, auth_headers, note, other_headers):
    audio_id = upload_audio(client, note["id"], auth_headers).json()["id"]
    assert client.get(f"/audio/{audio_id}", headers=other_headers).status_code == 404


def test_other_user_delete_audio_returns_404(client, auth_headers, note, other_headers):
    audio_id = upload_audio(client, note["id"], auth_headers).json()["id"]
    assert client.delete(f"/audio/{audio_id}", headers=other_headers).status_code == 404
    assert client.get(f"/audio/{audio_id}", headers=auth_headers).status_code == 200
