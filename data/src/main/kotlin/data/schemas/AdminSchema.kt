package data.schemas

import data.Db
import data.models.*

object AdminSchema {
    fun ensure() {
        // Таблица banned_users уже создается в ProfileSchema
        // Модуль зарезервирован для будущего расширения админских функций
    }
}
