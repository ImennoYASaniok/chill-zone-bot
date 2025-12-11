package core

enum class States {
    BaseMenu,
    MemeMenu,
    PredictionsMenu,
    CollectionsMenu
}

var currState: States = States.BaseMenu