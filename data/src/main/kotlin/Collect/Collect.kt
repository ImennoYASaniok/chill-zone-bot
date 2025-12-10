package Collect

import ParseApiJson.*


fun main() {
    for (i in 1..80) {
        val rawJson = getFromApi(250,i)
        val parsedJson = parseJson(rawJson)
        updateDatabase(parsedJson)
        Thread.sleep(600)
    }
}

