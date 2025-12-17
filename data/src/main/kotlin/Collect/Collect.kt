package Collect


fun main() {
    for (i in 1..30) {
        println("Current page: $i")
        val rawJson = KinoPart.getFromApi(250,i)
        val parsedJson = KinoPart.parseJson(rawJson)
        KinoPart.updateDatabase(parsedJson)
    }
}

