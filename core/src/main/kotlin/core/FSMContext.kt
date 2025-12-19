package core

enum class States {
    GeneralMenu,
    Settings,
    Account,
    MemeMenu,
    PredictionsMenu,
    CollectionsMenu,
    FeedbackMenu
}

var currState: States = States.GeneralMenu