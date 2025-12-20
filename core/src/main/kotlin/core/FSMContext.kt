package core

enum class States {
    GeneralMenu,
    Settings,
    Account,
    MemeMenu,
    PredictionsMenu,
    CollectionsMenu,
    FeedbackMenu,
    MiniGamesMenu,
    TestsMenu
}

var currState: States = States.GeneralMenu
