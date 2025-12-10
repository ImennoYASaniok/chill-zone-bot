package main

import BooksPart.collectResponse

fun main() {
    for (i in collectResponse("преступление и наказание")!!) {
        i.volumeInfo.title
        i.volumeInfo.description
        i.volumeInfo.authors
        i.volumeInfo.pageCount
        i.volumeInfo.categories
        // дата публикации обычно в description есть
    }
}