package cc.rbbl.link_handlers

import cc.rbbl.Artist
import cc.rbbl.MessageHandler
import cc.rbbl.ProgramConfig
import cc.rbbl.ResponseData
import cc.rbbl.exceptions.ParsingException
import com.adamratzman.spotify.SpotifyAppApi
import com.adamratzman.spotify.spotifyAppApi
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.runBlocking

class SpotifyMessageHandler(
    config: ProgramConfig, private val spotifyApi: SpotifyAppApi = runBlocking {
        spotifyAppApi(config.spotifyClientId, config.spotifyClientSecret).build()
    }
) : MessageHandler {

    override fun getGenreResponses(message: String): Set<ResponseData> = runBlocking {
        val results = HashSet<ResponseData>()
        val longUrls =
            SPOTIFY_LONG_LINK_PATTERN.findAll(message).map { it.value } + SPOTIFY_SHORT_LINK_PATTERN.findAll(message)
                .let {
                    val resolvedUrls = mutableListOf<String>()
                    for (match in it) {
                        val body = httpClient.get(match.value).bodyAsText()
                        SPOTIFY_LONG_LINK_PATTERN.find(body)?.let { matchResult ->
                            resolvedUrls.add(matchResult.value)
                        }
                    }
                    resolvedUrls
                }
        longUrls.forEach { url ->
            val typeAndId = extractTypeAndId(url)
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
                results.add(ResponseData(url = url, error = e))
            }
        }
        results
    }

    private suspend fun getGenresForTrack(trackId: String): ResponseData {
        val track = spotifyApi.tracks.getTrack(trackId) ?: throw IllegalArgumentException("Unknown track ID '$trackId'")
        return getGenresForAlbum(
            track.album.id, ResponseData(
                url = track.externalUrls.spotify,
                title = track.name,
                titleImageUrl = track.album.images.lastOrNull()?.url,
                imageHeightAndWidth = track.album.images.lastOrNull()?.width,
                artists = track.artists.map { Artist(it.name, it.externalUrls.spotify) },
                artistImageUrl = track.artists.firstOrNull()?.toFullArtist()?.images?.first()?.url
            )
        )
    }

    private suspend fun getGenresForAlbum(albumId: String, prefilledData: ResponseData?): ResponseData {
        val album = spotifyApi.albums.getAlbum(albumId) ?: throw IllegalArgumentException("Unknown album ID '$albumId'")
        val data = prefilledData ?: ResponseData(
            url = album.externalUrls.spotify,
            title = album.name,
            titleImageUrl = album.images.lastOrNull()?.url,
            imageHeightAndWidth = album.images.lastOrNull()?.width,
            artists = album.artists.map { Artist(it.name, it.externalUrls.spotify) },
            artistImageUrl = album.artists.firstOrNull()?.toFullArtist()?.images?.firstOrNull()?.url
        )
        return if (album.genres.isNotEmpty()) {
            data.metadata["Genres"] = album.genres
            data
        } else {
            if (album.artists.isNotEmpty()) {
                getGenresForArtist(album.artists.first().id, data)
            } else {
                throw UnknownError("albums always should have a artist. idk what happened")
            }
        }
    }

    internal suspend fun getGenresForArtist(artistId: String, prefilledData: ResponseData?): ResponseData {
        val artist =
            spotifyApi.artists.getArtist(artistId) ?: throw IllegalArgumentException("Unknown artist ID '$artistId'")
        val data = prefilledData ?: ResponseData(
            url = artist.externalUrls.spotify,
            title = artist.name,
            titleImageUrl = artist.images.lastOrNull()?.url,
            imageHeightAndWidth = artist.images.lastOrNull()?.width
        )
        if (artist.genres.isNotEmpty()) {
            data.metadata["Genres"] = artist.genres
        }
        return data
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