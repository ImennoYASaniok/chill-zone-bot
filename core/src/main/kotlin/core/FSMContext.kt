package core

enum class States {
    GeneralMenu,
    MemeMenu,
    PredictionsMenu,
    CollectionsMenu
}

var currState: States = States.GeneralMenu