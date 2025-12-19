package core.minigames.tictactoe

import core.currentChatId

object TicTacToeService {
    fun render(): String {
        val game = TicTacToeStore.get(currentChatId)
        fun cell(i: Int): String = when (game.board[i]) {
            'X' -> "❌"
            'O' -> "⭕️"
            else -> "⬜️"
        }

        val boardText =
            "${cell(0)}${cell(1)}${cell(2)}\n" +
                    "${cell(3)}${cell(4)}${cell(5)}\n" +
                    "${cell(6)}${cell(7)}${cell(8)}"

        return "Крестики-нолики\n\n$boardText"
    }
}