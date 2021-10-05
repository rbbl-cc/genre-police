package cc.rbbl

data class GenreResponse(val title: String, val genres: List<String>, val noGenresFound:Boolean = false)
