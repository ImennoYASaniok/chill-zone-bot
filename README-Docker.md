# Файлы настройки для Chill Zone Bot

Поместите Docker-файлы в корень проекта рядом с `build.gradle.kts`, `app/`, `core/` и `data/`.

## Запуск

1. Скопируйте `.env.example` в `.env`
2. Заполните `BOT_TOKEN`
3. При необходимости поменяйте `DATABASE_USER`, `DATABASE_PASSWORD`, `POSTGRES_DB`
4. Запустите контейнеры:

```bash
docker compose up --build
```

## Остановка

```bash
docker compose down
```

## Проверка логов

Если бот не отвечает, откройте логи контейнера:

```bash
docker compose logs -f bot
```

После исправления entrypoint в логах должны появляться:

- сообщения об инициализации схемы и сидов
- сообщение о старте polling
- запись о каждом входящем сообщении от Telegram

## Примечания

- Внутри Docker Postgres доступен по адресу `db:5432`
- На хосте база доступна по `localhost:5430`
- Для локального запуска из IDE удобно использовать `DATABASE_URL=jdbc:postgresql://localhost:5430/chill_zone`
- Внутри контейнера `docker-compose.yml` автоматически подменяет `DATABASE_URL` на `jdbc:postgresql://db:5432/${POSTGRES_DB}`
