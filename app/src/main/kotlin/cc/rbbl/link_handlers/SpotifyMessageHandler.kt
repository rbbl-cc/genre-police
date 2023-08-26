package cc.rbbl.link_handlers

import cc.rbbl.GenreResponse
import cc.rbbl.MessageHandler
import cc.rbbl.ProgramConfig
import cc.rbbl.exceptions.NoGenreFoundException
import cc.rbbl.exceptions.ParsingException
import com.adamratzman.spotify.SpotifyAppApi
import com.adamratzman.spotify.spotifyAppApi
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.runBlocking

class SpotifyMessageHandler(config: ProgramConfig) : MessageHandler {
    private var spotifyApi: SpotifyAppApi

    init {
        runBlocking {
            spotifyApi = spotifyAppApi(config.spotifyClientId, config.spotifyClientSecret).build()
        }
    }

    override fun getGenreResponses(message: String): Set<GenreResponse> = runBlocking {
        val results = HashSet<GenreResponse>()
        val longUrls =
            SPOTIFY_LONG_LINK_PATTERN.findAll(message).map { it.value } + SPOTIFY_SHORT_LINK_PATTERN.findAll(message)
                .let {
                    val resolvedUrls = mutableListOf<String>()
                    for (match in it) {
                        val body = httpClient.get(match.value).bodyAsText()
                        SPOTIFY_LONG_LINK_PATTERN.find(body)?.let {
                            resolvedUrls.add(it.value)
                        }
                    }
                    resolvedUrls
                }
        longUrls.forEach {
            val typeAndId = extractTypeAndId(it)
            try {
                if (typeAndId.size != 2) {
                    throw ParsingException()
                }
                when (typeAndId[0].lowercase()) {
                    "track" -> results.add(getGenresForTrack(typeAndId[1]))
                    "album" -> results.add(getGenresForAlbum(typeAndId[1], null))
                    "artist" -> results.add(getGenresForArtist(typeAndId[1], null))
                }
            } catch (e: Exception) {
                when (e) {
                    is NoGenreFoundException -> results.add(GenreResponse(e.itemName, listOf(), e))
                    is IllegalArgumentException -> results.add(
                        GenreResponse(
                            "${typeAndId[0]}/${typeAndId[1]}",
                            listOf(),
                            e
                        )
                    )

                    is ParsingException -> results.add(GenreResponse(it, listOf(), e))
                    else -> results.add(GenreResponse(it, listOf(), e))
                }
            }
        }
        results
    }

    private suspend fun getGenresForTrack(trackId: String): GenreResponse {
        val track = spotifyApi.tracks.getTrack(trackId)
        return if (track == null) {
            throw IllegalArgumentException(trackId)
        } else {
            getGenresForAlbum(track.album.id, track.name)
        }
    }

    private suspend fun getGenresForAlbum(albumId: String, title: String?): GenreResponse {
        val album = spotifyApi.albums.getAlbum(albumId)
        return if (album == null) {
            throw IllegalArgumentException(albumId)
        } else {
            if (album.genres.isNotEmpty()) {
                GenreResponse(title!!, album.genres.toList())
            } else {
                if (album.artists.isNotEmpty()) {
                    getGenresForArtist(album.artists.first().id, title ?: album.name)
                } else {
                    throw UnknownError("albums always should have a artist. idk what happened")
                }
            }
        }
    }

    private suspend fun getGenresForArtist(artistId: String, title: String?): GenreResponse {
        val artist = spotifyApi.artists.getArtist(artistId)
        return if (artist == null) {
            throw IllegalArgumentException("Unknown ID '$artistId'")
        } else {
            if (artist.genres.isNotEmpty()) {
                GenreResponse(title ?: artist.name, artist.genres.toList())
            } else {
                throw NoGenreFoundException(title ?: artist.name)
            }
        }
    }


    companion object {
        internal val SPOTIFY_LONG_LINK_PATTERN =
            Regex("\\bhttps://open\\.spotify\\.com/[-a-zA-Z]*/?(?:track|album|artist)[^ \\t\\n\\r]*\\b")
        internal val SPOTIFY_SHORT_LINK_PATTERN = Regex("https://spotify\\.link/[^ \\t\\n\\r]*")
        private const val SPOTIFY_DOMAIN = "https://open.spotify.com/"
        private val httpClient = HttpClient()

        internal fun extractTypeAndId(url: String?): Array<String> {
            if (url == null) {
                return emptyArray()
            }
            val typeSlashId = url.split("?")[0].replace(SPOTIFY_DOMAIN, "")
            return typeSlashId.split("/").toTypedArray().let { typeAndId ->
                if (typeAndId.size == 3) {
                    typeAndId.slice(1..2).toTypedArray()
                } else {
                    typeAndId
                }
            }
        }
    }
}