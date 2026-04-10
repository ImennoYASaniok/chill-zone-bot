package data.schemas

import data.Db

object Schema {
    fun ensure() {
        // Вызываем все модульные схемы в правильном порядке
        // Сначала профиль пользователя (users должен быть создан первым)
        ProfileSchema.ensure()
        
        // Затем остальные модули
        GamesSchema.ensure()
        MemesSchema.ensure()
        PredictionsSchema.ensure()
        CollectionsSchema.ensure()
        TestSchema.ensure()
        EventsSchema.ensure()
        FeedbackSchema.ensure()
        PixelArtSchema.ensure()
        
        // Системные миграции
        runMigrations()
    }
    
    private fun runMigrations() {
        // Миграция для добавления url в user_favorites
        try {
            Db.raw("alter table user_favorites add column if not exists url text")
        } catch (e: Exception) {
            // Игнорируем ошибку, если колонка уже существует
            println("Migration for url column in user_favorites: ${e.message}")
        }
        
        // Миграция для добавления description в user_favorites
        try {
            Db.raw("alter table user_favorites add column if not exists description text")
        } catch (e: Exception) {
            // Игнорируем ошибку, если колонка уже существует
            println("Migration for description column in user_favorites: ${e.message}")
        }
        
        // Миграция для добавления hide_username в users
        try {
            Db.raw("alter table users add column if not exists hide_username boolean not null default false")
        } catch (e: Exception) {
            // Игнорируем ошибку, если колонка уже существует
            println("Migration for hide_username column in users: ${e.message}")
        }
    }
}
