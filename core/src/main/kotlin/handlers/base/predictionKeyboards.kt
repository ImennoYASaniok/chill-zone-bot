package ChillZoneBot.core.src.main.kotlin.handlers.predictions


import ChillZoneBot.core.src.main.kotlin.ReplyClass.Button

// Клавиатура главного меню
fun getMainKeyboard(): MutableList<MutableList<Button>> {
    val getPrediction = Button("Получить предсказание")
    val myPredictions = Button("Мои предсказания")
    val addPrediction = Button("Добавить предсказание")
    val searchPrediction = Button("Поиск предсказаний")

    return mutableListOf(
        mutableListOf(getPrediction),
        mutableListOf(myPredictions),
        mutableListOf(addPrediction),
        mutableListOf(searchPrediction)
    )
}

// Клавиатура выбора редкости при добавлении предсказания
fun getRarityKeyboard(): MutableList<MutableList<Button>> {
    val common = Button("Обычное")
    val rare = Button("Редкое")
    val epic = Button("Эпическое")
    val legendary = Button("Легендарное")
    val back = Button("Назад")

    return mutableListOf(
        mutableListOf(common, rare),
        mutableListOf(epic, legendary),
        mutableListOf(back)
    )
}
