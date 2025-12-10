package ChillZoneBot.app.src.main.kotlin.App

import java.util.Collections.*


class Test(
    var name: String,
    var author: String,
    var maxScore: Int,
    var minScore: Int,
    var resultOfTest: MutableMap<IntRange, String>,
    var answerToScore: MutableMap<Int, MutableMap<Int, Int>> // answerToScore[a][b] = c, где a - номер вопроса, b - номер ответа, c - баллы за ответ

    ) {

    fun updateEdgeValues() {
        var newMax = 0
        var newMin = 0
        for (i in answerToScore.values) {
            newMax += max(i.values)
            newMin += min(i.values)
        }
        maxScore = newMax
        minScore = newMin
    }
}