import os


def delete_file(path: str) -> None:
    """Удаляет файл с диска; если его уже нет — не ошибка."""
    try:
        os.remove(path)
    except FileNotFoundError:
        pass
