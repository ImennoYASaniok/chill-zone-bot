package core

enum class States {
    GeneralMenu,
    Settings,
    Account,
    MemeMenu,
    PredictionsMenu,
    CollectionsMenu,
    MiniGamesMenu,
    TicTacToe
}

var currState: States = States.GeneralMenu