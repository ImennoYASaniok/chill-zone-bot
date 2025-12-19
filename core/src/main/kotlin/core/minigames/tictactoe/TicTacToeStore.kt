package core.minigames.tictactoe

data class TicTacToeGame(
    val board: CharArray = CharArray(9) { ' ' },
    var turn: Char = 'X',
    var finished: Boolean = false,
    var winner: Char? = null,
    var mode: Int = 2
)

object TicTacToeStore {
    private val games = mutableMapOf<Long, TicTacToeGame>()

    fun get(chatId: Long): TicTacToeGame {
        return games.getOrPut(chatId) { TicTacToeGame() }
    }

    fun reset(chatId: Long, mode: Int? = null): TicTacToeGame {
        val g = TicTacToeGame(mode = mode ?: (games[chatId]?.mode ?: 2))
        games[chatId] = g
        return g
    }

    fun setMode(chatId: Long, mode: Int): TicTacToeGame {
        val g = games.getOrPut(chatId) { TicTacToeGame() }
        g.mode = mode
        reset(chatId, mode)
        return games[chatId]!!
    }
}