package Collect


fun main() {
    for (i in 1..80) {
        val rawJson = KinoPart.getFromApi(250,i)
        val parsedJson = KinoPart.parseJson(rawJson)
        KinoPart.updateDatabase(parsedJson)
        Thread.sleep(600)
    }
}

