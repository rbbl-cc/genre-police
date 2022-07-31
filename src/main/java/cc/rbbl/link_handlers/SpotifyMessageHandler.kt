package cc.rbbl.link_handlers

import cc.rbbl.GenreResponse
import cc.rbbl.MessageHandler
import cc.rbbl.ProgramConfig
import cc.rbbl.exceptions.NoGenreFoundException
import cc.rbbl.exceptions.ParsingException
import com.adamratzman.spotify.SpotifyAppApi
import com.adamratzman.spotify.spotifyAppApi
import kotlinx.coroutines.runBlocking
import java.util.regex.Pattern

class SpotifyMessageHandler(config: ProgramConfig) : MessageHandler {
    private var spotifyApi: SpotifyAppApi

    init {
        runBlocking {
            spotifyApi = spotifyAppApi(config.spotifyClientId, config.spotifyClientSecret).build()
        }
    }

    override fun getGenreResponses(message: String): Set<GenreResponse> = runBlocking {
        val results = HashSet<GenreResponse>()
        val matcher = Pattern.compile(SPOTIFY_LINK_PATTERN).matcher(message)
        if (!message.lowercase().contains("genre")) {
            while (matcher.find()) {
                val typeSlashId = matcher.group().split("?")[0].replace(spotifyDomain, "")
                try {
                    val typeAndId = typeSlashId.split("/").toTypedArray()
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
                        is IllegalArgumentException -> results.add(GenreResponse(typeSlashId, listOf(), e))
                        is ParsingException -> results.add(GenreResponse(matcher.group(), listOf(), e))
                        else -> results.add(GenreResponse(matcher.group(), listOf(), e))
                    }
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
        private const val SPOTIFY_LINK_PATTERN = "\\bhttps://open.spotify.com/(?:track|album|artist)[^ \\t\\n\\r]*\\b"
        private const val spotifyDomain = "https://open.spotify.com/"
    }
}