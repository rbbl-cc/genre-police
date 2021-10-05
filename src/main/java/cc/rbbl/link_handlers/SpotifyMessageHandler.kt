package cc.rbbl.link_handlers

import cc.rbbl.GenreResponse
import cc.rbbl.MessageHandler
import cc.rbbl.exceptions.NoGenreFoundException
import cc.rbbl.program_parameters_jvm.ParameterHolder
import com.wrapper.spotify.SpotifyApi
import com.wrapper.spotify.exceptions.SpotifyWebApiException
import com.wrapper.spotify.exceptions.detailed.UnauthorizedException
import org.apache.hc.core5.http.ParseException
import java.io.IOException
import java.util.regex.Pattern

class SpotifyMessageHandler(parameters: ParameterHolder) : MessageHandler {
    private val spotifyApi: SpotifyApi
    private var retryCounter = 0

    override fun getGenreResponses(message: String): Set<GenreResponse> {
        try {
            val results = HashSet<GenreResponse>()
            val matcher = Pattern.compile(SPOTIFY_LINK_PATTERN).matcher(message)
            if (!message.lowercase().contains("genre")) {
                while (matcher.find()) {
                    try {
                        val typeSlashId = matcher.group().split("\\?").toTypedArray()[0].replace(spotifyDomain, "")
                        when (typeSlashId.split("/").toTypedArray()[0].lowercase()) {
                            "track" -> results.add(
                                getGenresForTrack(typeSlashId.split("/").toTypedArray()[1])!!
                            )
                            "album" -> results.add(getGenresForAlbum(typeSlashId.split("/").toTypedArray()[1], null))
                            "artist" -> results.add(getGenresForArtist(typeSlashId.split("/").toTypedArray()[1], null))
                        }
                    } catch (e: NoGenreFoundException) {
                        results.add(GenreResponse(e.itemName, "Spotify has no genre for that Item"))
                    }
                }
            }
            return results
        } catch (e: UnauthorizedException) {
            if (retryCounter < 3) {
                retryCounter++
                refreshSpotifyToken()
                return getGenreResponses(message)
            }
        }
        throw UnknownError()
    }

    private fun getGenresForTrack(trackId: String): GenreResponse? {
        try {
            val track = spotifyApi.getTrack(trackId).build().execute()
            return getGenresForAlbum(track.album.id, track.name)
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: SpotifyWebApiException) {
            if (e is UnauthorizedException) {
                throw e
            } else {
                e.printStackTrace()
            }
        } catch (e: ParseException) {
            e.printStackTrace()
        }
        return null
    }

    private fun getGenresForAlbum(albumId: String, title: String?): GenreResponse {
        try {
            val album = spotifyApi.getAlbum(albumId).build().execute()
            if (album.genres.isNotEmpty()) {
                return GenreResponse(title!!, genresToMessage(album.genres)!!)
            } else {
                for (artistSimplified in album.artists) {
                    return getGenresForArtist(artistSimplified.id, title ?: album.name)
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: SpotifyWebApiException) {
            if (e is UnauthorizedException) {
                throw e
            } else {
                e.printStackTrace()
            }
        } catch (e: ParseException) {
            e.printStackTrace()
        }
        throw UnknownError()
    }

    private fun getGenresForArtist(artistId: String, title: String?): GenreResponse {
        try {
            val artist = spotifyApi.getArtist(artistId).build().execute()
            if (artist.genres.isNotEmpty()) {
                return GenreResponse(title ?: artist.name, genresToMessage(artist.genres)!!)
            }
            throw NoGenreFoundException(title ?: artist.name)
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: SpotifyWebApiException) {
            if (e is UnauthorizedException) {
                throw e
            } else {
                e.printStackTrace()
            }
        } catch (e: ParseException) {
            e.printStackTrace()
        }
        throw UnknownError()
    }

    private fun refreshSpotifyToken() {
        try {
            val clientCredentials = spotifyApi.clientCredentials().build().execute()
            spotifyApi.accessToken = clientCredentials.accessToken
            retryCounter = 0
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: SpotifyWebApiException) {
            e.printStackTrace()
        } catch (e: ParseException) {
            e.printStackTrace()
        }
    }

    private fun genresToMessage(genres: Array<String>?): String? {
        if (genres == null || genres.isEmpty()) {
            return null
        }
        val message = StringBuilder()
        for (genre in genres) {
            message.append("\"").append(genre).append("\" ")
        }
        return message.toString()
    }

    companion object {
        private const val SPOTIFY_LINK_PATTERN = "\\bhttps://open.spotify.com/(?:track|album|artist)[^ \\t\\n\\r]*\\b"
        private const val spotifyDomain = "https://open.spotify.com/"
    }

    init {
        spotifyApi = SpotifyApi.Builder().setClientId(parameters["SPOTIFY_CLIENT_ID"])
            .setClientSecret(parameters["SPOTIFY_CLIENT_SECRET"]).build()
        val clientCredentials = spotifyApi.clientCredentials().build().execute()
        spotifyApi.accessToken = clientCredentials.accessToken
    }
}