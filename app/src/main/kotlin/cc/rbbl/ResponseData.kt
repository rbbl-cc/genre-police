package cc.rbbl

data class ResponseData(
    val url: String? = null,
    val title: String? = null,
    val titleImageUrl: String? = null,
    val imageHeightAndWidth: Int? = null,
    val authorUrl: String? = null,
    val authors: List<String>? = null,
    val authorImageUrl: String? = null,
    val metadata: MutableMap<String,List<String>> = mutableMapOf(),
    val error: Exception? = null
) {
    fun getDescription(): String? {
        if (authors == null) {
            return null
        }
        return authors.takeLast(authors.size-1).fold("feat. ") {acc, s -> "$acc$s, " }.trimEnd(',', ' ')
    }
}
