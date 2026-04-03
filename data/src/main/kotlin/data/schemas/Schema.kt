package data.schemas

// Импортируем Db из родительского пакета
import data.Db

object Schema {
    fun ensure() {
        // Вызываем все модульные схемы
        UserSchema.ensure()
        ContentSchema.ensure()
        EventsSchema.ensure()
        FavoritesSchema.ensure()
        TestSchema.ensure()
        FeedbackSchema.ensure()
        
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
        
        // Миграция для добавления hide_username в существующие таблицы
        Db.raw("""
            alter table users add column if not exists hide_username boolean not null default false
        """)
    }
}