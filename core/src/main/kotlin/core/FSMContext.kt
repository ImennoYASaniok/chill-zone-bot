package core

enum class States {
    GeneralMenu,
    Settings,
    MemeMenu,
    PredictionsMenu,
    CollectionsMenu
}

var currState: States = States.GeneralMenu