package core.keyboards

import core.Button

fun getKeyboardTicTacToe(): MutableList<MutableList<Button>> {
    val pvp = Button(name = "👥 2 игрока")
    val solo = Button(name = "🤖 против бота")
    val restart = Button(name = "🔁 Новая игра")

    return mutableListOf(
        mutableListOf(Button("1"), Button("2"), Button("3")),
        mutableListOf(Button("4"), Button("5"), Button("6")),
        mutableListOf(Button("7"), Button("8"), Button("9")),
        mutableListOf(pvp, solo),
        mutableListOf(restart)
    )
}