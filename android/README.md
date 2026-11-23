# Android-клиент Voice Notes

Приложение на Kotlin и Jetpack Compose. Работает с сервером из папки `backend` (адреса и правила описаны в `docs/spec-stage2.md`).

## Экраны
- вход и регистрация;
- список заметок;
- просмотр заметки с проигрывателем записей;
- форма создания и изменения заметки с записью голоса.

## Структура
```
app/src/main/java/com/voicenotes/app/
├── MainActivity.kt        навигация между экранами
├── VoiceNotesApp.kt       общие объекты приложения
├── data/                  Retrofit-интерфейс, модели, репозиторий, хранение токена
├── audio/                 запись (MediaRecorder) и воспроизведение (ExoPlayer)
└── ui/                    экраны Compose и ViewModel
```

## Запуск
1. Запустите сервер из корня проекта: `docker compose up --build -d`.
2. Соберите и установите приложение (нужны Android Studio или Android SDK и JDK 17+):
   ```bash
   cd android
   ./gradlew :app:assembleDebug
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   adb shell am start -n com.voicenotes.app/.MainActivity
   ```
   Либо откройте папку `android` в Android Studio и нажмите Run.
3. Адрес сервера задан в `data/VoiceNotesApi.kt` (`ApiClient.BASE_URL`):
   - эмулятор: `http://10.0.2.2:8000/` (уже стоит);
   - настоящий телефон: подставьте IP компьютера в той же сети Wi-Fi, например `http://192.168.0.10:8000/`.

Обычный HTTP разрешён только в debug-сборке (`app/src/debug/AndroidManifest.xml`).

## Эмулятор на Mac (Apple Silicon)
Если эмулятор зависает при записи звука, запускайте его без звука: `emulator -avd <имя> -no-audio`. Запись при этом идёт с виртуального микрофона.

## Что не вошло в эту версию
Локальный кеш Room и офлайн-очередь изменений (без связи с сервером приложение показывает сообщение об ошибке).
