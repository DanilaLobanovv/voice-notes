def test_register_returns_201(client):
    response = client.post("/auth/register", json={"email": "new@example.com", "password": "password123"})
    assert response.status_code == 201
    body = response.json()
    assert body["email"] == "new@example.com"
    assert "password" not in body and "password_hash" not in body


def test_register_duplicate_email_returns_409(client):
    data = {"email": "dup@example.com", "password": "password123"}
    client.post("/auth/register", json=data)
    response = client.post("/auth/register", json=data)
    assert response.status_code == 409


def test_register_short_password_returns_422(client):
    response = client.post("/auth/register", json={"email": "short@example.com", "password": "1234567"})
    assert response.status_code == 422


def test_register_lowercases_email(client):
    response = client.post("/auth/register", json={"email": "MiXeD@Example.COM", "password": "password123"})
    assert response.status_code == 201
    assert response.json()["email"] == "mixed@example.com"


def test_duplicate_email_in_other_case_returns_409(client):
    client.post("/auth/register", json={"email": "case@example.com", "password": "password123"})
    response = client.post("/auth/register", json={"email": "CASE@example.com", "password": "password123"})
    assert response.status_code == 409


def test_login_returns_token(client):
    client.post("/auth/register", json={"email": "login@example.com", "password": "password123"})
    response = client.post("/auth/login", data={"username": "login@example.com", "password": "password123"})
    assert response.status_code == 200
    body = response.json()
    assert body["access_token"]
    assert body["token_type"] == "bearer"


def test_login_wrong_password_returns_401(client):
    client.post("/auth/register", json={"email": "wrong@example.com", "password": "password123"})
    response = client.post("/auth/login", data={"username": "wrong@example.com", "password": "not-the-password"})
    assert response.status_code == 401


def test_login_unknown_email_returns_401(client):
    response = client.post("/auth/login", data={"username": "nobody@example.com", "password": "password123"})
    assert response.status_code == 401


def test_me_with_token_returns_user(client, auth_headers):
    response = client.get("/auth/me", headers=auth_headers)
    assert response.status_code == 200
    assert response.json()["email"] == "alice@example.com"


def test_me_without_token_returns_401(client):
    response = client.get("/auth/me")
    assert response.status_code == 401


def test_me_with_bad_token_returns_401(client):
    response = client.get("/auth/me", headers={"Authorization": "Bearer not-a-real-token"})
    assert response.status_code == 401
