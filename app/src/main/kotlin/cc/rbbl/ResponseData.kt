package cc.rbbl

data class ResponseData(
    val url: String? = null,
    val title: String? = null,
    val titleImageUrl: String? = null,
    val imageHeightAndWidth: Int? = null,
    val artists: List<Artist>? = null,
    val artistImageUrl: String? = null,
    val metadata: MutableMap<String, List<String>> = mutableMapOf(),
    val error: Exception? = null
) {
    fun getDescription(): String? {
        if (artists == null || artists.size <= 1) {
            return null
        }
        val additionalAuthors = artists.let {
            it.takeLast(it.size - 1)
        }
        return additionalAuthors.fold("feat. ") { acc, s -> "$acc${s.toMarkdown()}, " }.trimEnd(',', ' ')
    }
}

data class Artist(val name: String, val url: String? = null) {
    fun toMarkdown(): String = if (url == null) {
        name
    } else {
        "[$name]($url)"
    }
}
