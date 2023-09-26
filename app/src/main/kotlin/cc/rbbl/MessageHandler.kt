package cc.rbbl

interface MessageHandler {
    fun getGenreResponses(message: String): Set<ResponseData>
}