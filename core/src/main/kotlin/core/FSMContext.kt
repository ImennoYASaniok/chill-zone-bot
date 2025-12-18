package core

enum class States {
    GeneralMenu,
    Settings,
    Account,
    MemeMenu,
    PredictionsMenu,
    CollectionsMenu
}

var currState: States = States.GeneralMenu