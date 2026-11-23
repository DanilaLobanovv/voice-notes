def test_create_note_returns_201(client, auth_headers):
    response = client.post("/notes", json={"title": "Idea", "text": "write tests"}, headers=auth_headers)
    assert response.status_code == 201
    body = response.json()
    assert body["title"] == "Idea"
    assert body["text"] == "write tests"


def test_create_note_without_text(client, auth_headers):
    response = client.post("/notes", json={"title": "Only title"}, headers=auth_headers)
    assert response.status_code == 201
    assert response.json()["text"] is None


def test_create_note_empty_title_returns_422(client, auth_headers):
    response = client.post("/notes", json={"title": ""}, headers=auth_headers)
    assert response.status_code == 422


def test_create_note_long_title_returns_422(client, auth_headers):
    response = client.post("/notes", json={"title": "x" * 121}, headers=auth_headers)
    assert response.status_code == 422


def test_create_note_title_of_120_chars_is_ok(client, auth_headers):
    response = client.post("/notes", json={"title": "x" * 120}, headers=auth_headers)
    assert response.status_code == 201


def test_notes_require_token(client):
    assert client.get("/notes").status_code == 401


def test_list_notes(client, auth_headers, note):
    response = client.get("/notes", headers=auth_headers)
    assert response.status_code == 200
    assert [n["id"] for n in response.json()] == [note["id"]]


def test_get_note_by_id(client, auth_headers, note):
    response = client.get(f"/notes/{note['id']}", headers=auth_headers)
    assert response.status_code == 200
    assert response.json()["title"] == "Shopping"
    assert response.json()["audio_records"] == []


def test_get_missing_note_returns_404(client, auth_headers):
    response = client.get("/notes/00000000-0000-0000-0000-000000000000", headers=auth_headers)
    assert response.status_code == 404


def test_update_note(client, auth_headers, note):
    response = client.put(f"/notes/{note['id']}", json={"title": "Groceries"}, headers=auth_headers)
    assert response.status_code == 200
    assert response.json()["title"] == "Groceries"
    assert response.json()["text"] == "milk"  # текст не прислали — он не изменился


def test_update_note_empty_title_returns_422(client, auth_headers, note):
    response = client.put(f"/notes/{note['id']}", json={"title": ""}, headers=auth_headers)
    assert response.status_code == 422


def test_update_note_null_title_returns_422(client, auth_headers, note):
    response = client.put(f"/notes/{note['id']}", json={"title": None}, headers=auth_headers)
    assert response.status_code == 422


def test_delete_note(client, auth_headers, note):
    response = client.delete(f"/notes/{note['id']}", headers=auth_headers)
    assert response.status_code == 204
    assert client.get(f"/notes/{note['id']}", headers=auth_headers).status_code == 404
