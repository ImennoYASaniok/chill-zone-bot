package core

enum class PendingAction {
    NONE,
    EDIT_NAME,
    EDIT_BIO,
    ADD_MEME,
    FEEDBACK_TEXT,
    ADD_PREDICTION_TEXT,
    ADD_PREDICTION_RARITY,
    SEARCH_PREDICTIONS,
    CREATE_TEST_TITLE,
    CREATE_TEST_KIND,
    CREATE_TEST_PROMPT,
    CREATE_TEST_MORE,
    PLAY_TEST,
    CREATE_EVENT_TITLE,
    CREATE_EVENT_DESC,
    CREATE_EVENT_PLACE,
    CREATE_EVENT_TIME,
    CREATE_EVENT_MAX,
    CREATE_EVENT_KIND,
    COLLECTION_QUERY,
    COLLECTION_RESULTS,  // Результаты поиска
    SEARCH_TYPE_SELECT,  // Выбор типа поиска (фильмы, сериалы и т.д.)
    RPS_CHOICE,
    VIEW_FAVORITES,  // Новый action для просмотра избранных с навигацией
    
    // Админские действия
    ADMIN_USER_LIST,
    ADMIN_BANNED_LIST,
    ADMIN_SEARCH,
    ADMIN_SEARCH_RESULTS
}

data class Session(
    var action: PendingAction = PendingAction.NONE,
    val data: MutableMap<String, String> = mutableMapOf(),
    var context: FSMContext = FSMContext.MEMES  // По умолчанию контекст мемов
)

object SessionStore {
    private val sessions = mutableMapOf<Long, Session>()

    fun get(chatId: Long): Session = sessions.getOrPut(chatId) { Session() }

    fun clear(chatId: Long) {
        sessions.remove(chatId)
    }
}
