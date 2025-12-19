package Collect

import ParseApiJson.*


fun main() {
    for (i in 1..80) {
        parseDetailedMovieJson(getFromApi(250,i))
        Thread.sleep(600)
    }
}

