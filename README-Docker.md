# Файлы настройки для Chill Zone Bot

Поместите эти файлы в корневой каталог проекта рядом с папками "build.gradle.kts" и "app/", "core/", "data/".

## Запуск

1. Скопируйте ".env.example" в ".env"
2. Введите "BOT_TOKEN", "DATABASE_USER", `DATABASE_PASSWORD`
3. Запустите все:

```bash
docker-compose up --build
```

## Удаление контейнеров

```bash
docker-compose down
```

## Примечания

- Внутри Docker Postgres доступен как "db:5432".
- На вашем хост-компьютере Postgres отображается как `localhost:5430`.
- Бот читает ".env", поэтому файл compose монтирует его в контейнер.
- `DATABASE_URL` в compose заменен на `jdbc:postgresql://db:5432/chill_zone`.