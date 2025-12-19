package core.handlers.miniGames
import java.awt.Button



/* ===============================
   Главное меню
================================ */

fun gameMainKeyboard(): MutableList<MutableList<Button>> {
    return mutableListOf(
        mutableListOf(
            Button("🎡 Прокрутить колесо")
        ),
        mutableListOf(
            Button("📊 Моя статистика"),
            Button("🏆 Топ 10")
        )
    )
}

/* ===============================
   Выбор стикера / игры
================================ */

fun stickersKeyboard(): MutableList<MutableList<Button>> {
    return mutableListOf(
        mutableListOf(
            Button("⚽ Гол"),
            Button("🏀 В кольцо")
        ),
        mutableListOf(
            Button("🎰 Колесо фортуны")
        ),
        mutableListOf(
            Button("⬅ Назад")
        )
    )
}





