package core.utils


object Url {
    val TYPE_IMGS = mutableListOf("png", "jpg", "jpeg", "webp", )
    val TYPE_ANIMATION = "gif"
    val TYPE_VIDEOS = mutableListOf("mp4", "mov", "avi", "mkv")
    val TYPE_AUDIO = mutableListOf("mp3", "m4a", "ogg", "opus", "flac", "wav")

    fun determineTypeUrl(url: String): String {
        val urlSplitByColon = url.split(":")
        if (urlSplitByColon[0] == "https" && urlSplitByColon[0][1] == '/' && urlSplitByColon[0][2] == '/') {
            return "link"
        }
        else {
            return "path"
        }
    }

    fun determineTypeFile(url: String): String {
        val resultSplit = url.split(".")
        return resultSplit[resultSplit.size - 1]
    }

    fun formatUrl(url: String): String {
        return "core/messageAssets/$url"
    }
}