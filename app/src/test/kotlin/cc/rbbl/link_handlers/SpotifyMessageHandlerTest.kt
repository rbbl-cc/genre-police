package cc.rbbl.link_handlers

import cc.rbbl.ProgramConfig
import cc.rbbl.ResponseData
import com.adamratzman.spotify.SpotifyAppApi
import com.adamratzman.spotify.models.*
import io.mockk.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class SpotifyMessageHandlerTest {

    @Nested
    inner class LongLinkPattern {

        @Test
        fun `regular track url`() {
            assertTrue {
                SpotifyMessageHandler.SPOTIFY_LONG_LINK_PATTERN.matches("https://open.spotify.com/track/2p4p9YGwmJIdf5IA9sSWhm?si=ac6b3105df714569")
            }
        }

        @Test
        fun `regular anonymous track url`() {
            assertTrue {
                SpotifyMessageHandler.SPOTIFY_LONG_LINK_PATTERN.matches("https://open.spotify.com/track/2p4p9YGwmJIdf5IA9sSWhm")
            }
        }

        @Test
        fun `regular album url`() {
            assertTrue {
                SpotifyMessageHandler.SPOTIFY_LONG_LINK_PATTERN.matches("https://open.spotify.com/album/5WS1g0cKtjfK6eDoSLdv7d?si=ff8085a125f24d7d")
            }
        }

        @Test
        fun `regular anonymous album url`() {
            assertTrue {
                SpotifyMessageHandler.SPOTIFY_LONG_LINK_PATTERN.matches("https://open.spotify.com/album/5WS1g0cKtjfK6eDoSLdv7d")
            }
        }

        @Test
        fun `regular artist url`() {
            assertTrue {
                SpotifyMessageHandler.SPOTIFY_LONG_LINK_PATTERN.matches("https://open.spotify.com/artist/2o8lOQRjzsSC8UdbNN88HN?si=F970p5vnSpeFWeYz1uFpwQ")
            }
        }

        @Test
        fun `regular anonymous artist url`() {
            assertTrue {
                SpotifyMessageHandler.SPOTIFY_LONG_LINK_PATTERN.matches("https://open.spotify.com/artist/2o8lOQRjzsSC8UdbNN88HN")
            }
        }

        @Test
        fun `regular episode url`() {
            assertFalse {
                SpotifyMessageHandler.SPOTIFY_LONG_LINK_PATTERN.matches("https://open.spotify.com/episode/0UJirr3XsKjh2VI18aM6Bj?si=f918ba9433f041f6")
            }
        }

        @Test
        fun `regular anonymous episode url`() {
            assertFalse {
                SpotifyMessageHandler.SPOTIFY_LONG_LINK_PATTERN.matches("https://open.spotify.com/episode/0UJirr3XsKjh2VI18aM6Bj")
            }
        }

        @Test
        fun `regular show url`() {
            assertFalse {
                SpotifyMessageHandler.SPOTIFY_LONG_LINK_PATTERN.matches("https://open.spotify.com/show/6KnaAHvqf0pgTs3Kw3qQTR?si=cad36accf40245b3")
            }
        }

        @Test
        fun `regular anonymous show url`() {
            assertFalse {
                SpotifyMessageHandler.SPOTIFY_LONG_LINK_PATTERN.matches("https://open.spotify.com/show/6KnaAHvqf0pgTs3Kw3qQTR")
            }
        }

        @Test
        fun `market track url`() {
            assertTrue {
                SpotifyMessageHandler.SPOTIFY_LONG_LINK_PATTERN.matches("https://open.spotify.com/intl-de/track/4awF9g7FMdeTxeD4OaSUIa?si=e6d6d7cc272c4873")
            }
        }

        @Test
        fun `market anonymous track url`() {
            assertTrue {
                SpotifyMessageHandler.SPOTIFY_LONG_LINK_PATTERN.matches("https://open.spotify.com/intl-de/track/4awF9g7FMdeTxeD4OaSUIa")
            }
        }
    }

    @Nested
    inner class ShortLinkPattern {
        @Test
        fun `short track url against shortPatter`() {
            assertTrue {
                SpotifyMessageHandler.SPOTIFY_SHORT_LINK_PATTERN.matches("https://spotify.link/elxIFeXj2Ab")
            }
        }

        @Test
        fun `regular anonymous track url against shortPatter`() {
            assertFalse {
                SpotifyMessageHandler.SPOTIFY_SHORT_LINK_PATTERN.matches("https://open.spotify.com/track/2p4p9YGwmJIdf5IA9sSWhm")
            }
        }
    }

    @Nested
    inner class ExtractTypeAndId {
        @Test
        fun `extract type and id from regular track url`() = runTest {
            assertContentEquals(
                arrayOf("track", "2p4p9YGwmJIdf5IA9sSWhm"),
                SpotifyMessageHandler.extractTypeAndId("https://open.spotify.com/track/2p4p9YGwmJIdf5IA9sSWhm?si=ac6b3105df714569")
            )
        }

        @Test
        fun `extract type and id from anonymous regular track url`() = runTest {
            assertContentEquals(
                arrayOf("track", "2p4p9YGwmJIdf5IA9sSWhm"),
                SpotifyMessageHandler.extractTypeAndId("https://open.spotify.com/track/2p4p9YGwmJIdf5IA9sSWhm")
            )
        }

        @Test
        fun `extract type and id from market track url`() = runTest {
            assertContentEquals(
                arrayOf("track", "4awF9g7FMdeTxeD4OaSUIa"),
                SpotifyMessageHandler.extractTypeAndId("https://open.spotify.com/intl-de/track/4awF9g7FMdeTxeD4OaSUIa?si=e6d6d7cc272c4873")
            )
        }

        @Test
        fun `extract type and id from anonymous market track url`() = runTest {
            assertContentEquals(
                arrayOf("track", "4awF9g7FMdeTxeD4OaSUIa"),
                SpotifyMessageHandler.extractTypeAndId("https://open.spotify.com/intl-de/track/4awF9g7FMdeTxeD4OaSUIa")
            )
        }
    }

    @Nested
    inner class SpotifyClientBased {

        @Nested
        inner class GenresForArtists {
            private val artistUrl = "https://spotify.com/artist/test"
            private val artistName = "testArtist"
            private val artistTitleImageUrl = "https"
            private val titleWidth = 100
            private val genreList = listOf("genre1", "genre2")

            private fun getDefaultArtist() = mockk<Artist>().also {
                every { it.externalUrls.spotify } returns artistUrl
                every { it.name } returns artistName
                val imageMock = listOf(mockk<SpotifyImage>().also {
                    every { it.url } returns artistTitleImageUrl
                    every { it.width } returns titleWidth
                })
                every { it.images } returns imageMock
                every { it.genres } returns emptyList()
            }

            @Test
            fun `wrong link`() = runTest {
                val testee = SpotifyMessageHandler(mockk<ProgramConfig>(), mockk<SpotifyAppApi>().also {
                    coEvery { it.artists.getArtist(any()) } returns null
                })
                assertThrows<IllegalArgumentException> {
                    runBlocking {
                        testee.getGenresForArtist("", null)
                    }
                }
            }

            @Test
            fun `default artist without prefilled data`() {
                val testee = SpotifyMessageHandler(mockk<ProgramConfig>(), mockk<SpotifyAppApi>().also {
                    coEvery { it.artists.getArtist(any()) } returns getDefaultArtist()
                })
                assertEquals(ResponseData(
                    url = artistUrl,
                    title = artistName,
                    titleImageUrl = artistTitleImageUrl,
                    imageHeightAndWidth = titleWidth
                ), runBlocking { testee.getGenresForArtist("", null) })
            }

            @Test
            fun `genre artist without prefilled data`() {
                val testee = SpotifyMessageHandler(mockk<ProgramConfig>(), mockk<SpotifyAppApi>().also {
                    coEvery { it.artists.getArtist(any()) } returns getDefaultArtist().also {
                        every { it.genres } returns genreList
                    }
                })
                assertEquals(ResponseData(
                    url = artistUrl,
                    title = artistName,
                    titleImageUrl = artistTitleImageUrl,
                    imageHeightAndWidth = titleWidth,
                    metadata = mutableMapOf("Genres" to genreList)
                ), runBlocking { testee.getGenresForArtist("", null) })
            }

            @Test
            fun `default artist with prefilled data`() {
                val testee = SpotifyMessageHandler(mockk<ProgramConfig>(), mockk<SpotifyAppApi>().also {
                    coEvery { it.artists.getArtist(any()) } returns getDefaultArtist()
                })
                assertEquals(ResponseData(), runBlocking { testee.getGenresForArtist("", ResponseData()) })
            }

            @Test
            fun `genre artist with prefilled data`() {
                val testee = SpotifyMessageHandler(mockk<ProgramConfig>(), mockk<SpotifyAppApi>().also {
                    coEvery { it.artists.getArtist(any()) } returns getDefaultArtist().also {
                        every { it.genres } returns genreList
                    }
                })
                assertEquals(ResponseData(
                    metadata = mutableMapOf("Genres" to genreList)
                ), runBlocking { testee.getGenresForArtist("", ResponseData()) })
            }
        }

        @Nested
        inner class GenresForAlbums {
            private val albumUrl = "https://spotify.com/album/test"
            private val albumName = "testAlbum"
            private val artistTitleImageUrl = "https"
            private val titleWidth = 100
            private val genreList = listOf("genre1", "genre2")
            private val artistName = "ArtistName"
            private val artistUrl = "https://spotify.com/artist/test"
            private val artistId = "artistId"

            private fun getDefaultAlbum() = mockk<Album>().also {
                every { it.externalUrls.spotify } returns albumUrl
                every { it.name } returns albumName
                val imageMock = listOf(mockk<SpotifyImage>().also {
                    every { it.url } returns artistTitleImageUrl
                    every { it.width } returns titleWidth
                })
                every { it.images } returns imageMock
                every { it.genres } returns emptyList()
                every { it.artists } returns listOf(mockk<SimpleArtist>().also {
                    every { it.id  } returns artistId
                    every { it.name } returns artistName
                    every { it.externalUrls.spotify } returns artistUrl
                    coEvery { it.toFullArtist() } returns mockk<Artist>().also {
                        every { it.images } returns emptyList()
                    }
                })
            }

            @Test
            fun `wrong link`() = runTest {
                val testee = SpotifyMessageHandler(mockk<ProgramConfig>(), mockk<SpotifyAppApi>().also {
                    coEvery { it.albums.getAlbum(any()) } returns null
                })
                assertThrows<IllegalArgumentException> {
                    runBlocking {
                        testee.getGenresForAlbum("", null)
                    }
                }
            }

            @Test
            fun `default album without without artists`() {
                val testee = SpotifyMessageHandler(mockk<ProgramConfig>(), mockk<SpotifyAppApi>().also {
                    coEvery { it.albums.getAlbum(any()) } returns getDefaultAlbum().also {
                        coEvery { it.artists } returns emptyList()
                    }
                })
                assertThrows<UnknownError>(message = "albums always should have a artist. idk what happened") {
                    runBlocking {
                        testee.getGenresForAlbum(
                            "",
                            null
                        )
                    }
                }
            }

            @Test
            fun `default album without prefilled data`() {
                val testee = spyk(SpotifyMessageHandler(mockk<ProgramConfig>(), mockk<SpotifyAppApi>().also {
                    coEvery { it.albums.getAlbum(any()) } returns getDefaultAlbum()
                })).also {
                    coEvery {it.getGenresForArtist(any(), any())} returnsArgument(1)
                }
                assertEquals(ResponseData(
                    url = albumUrl,
                    title = albumName,
                    titleImageUrl = artistTitleImageUrl,
                    imageHeightAndWidth = titleWidth,
                    artists = listOf(cc.rbbl.Artist(name = artistName, url = artistUrl))
                ), runBlocking { testee.getGenresForAlbum("", null) })
                coVerify(exactly = 1) { testee.getGenresForArtist(any(), any()) }
            }

            @Test
            fun `genre album without prefilled data`() {
                val testee = SpotifyMessageHandler(mockk<ProgramConfig>(), mockk<SpotifyAppApi>().also {
                    coEvery { it.albums.getAlbum(any()) } returns getDefaultAlbum().also {
                        every { it.genres } returns genreList
                    }
                })
                assertEquals(ResponseData(
                    url = albumUrl,
                    title = albumName,
                    titleImageUrl = artistTitleImageUrl,
                    imageHeightAndWidth = titleWidth,
                    artists = listOf(cc.rbbl.Artist(artistName, artistUrl)),
                    metadata = mutableMapOf("Genres" to genreList)
                ), runBlocking { testee.getGenresForAlbum("", null) })
            }

            @Test
            fun `default album with prefilled data`() {
                val testee = spyk(SpotifyMessageHandler(mockk<ProgramConfig>(), mockk<SpotifyAppApi>().also {
                    coEvery { it.albums.getAlbum(any()) } returns getDefaultAlbum()
                })).also {
                    coEvery { it.getGenresForArtist(any(), any()) } returnsArgument(1)
                }
                assertEquals(ResponseData(), runBlocking { testee.getGenresForAlbum("", ResponseData()) })
                coVerify(exactly = 1) { testee.getGenresForArtist(any(), any()) }
            }

            @Test
            fun `genre album with prefilled data`() {
                val testee = SpotifyMessageHandler(mockk<ProgramConfig>(), mockk<SpotifyAppApi>().also {
                    coEvery { it.albums.getAlbum(any()) } returns getDefaultAlbum().also {
                        every { it.genres } returns genreList
                    }
                })
                assertEquals(ResponseData(
                    metadata = mutableMapOf("Genres" to genreList)
                ), runBlocking { testee.getGenresForAlbum("", ResponseData()) })
            }
        }

        @Nested
        inner class GenresForTracks {
            private val trackUrl = "trackUrl"
            private val trackName = "testTrack"
            private val artistName = "ArtistName"
            private val artistUrl = "https://spotify.com/artist/test"
            private val artistId = "artistId"
            private val simpleAlbum = mockk<SimpleAlbum>().also {
                every { it.id } returns "albumId"
                every { it.images } returns emptyList()
            }

            private fun getDefaultTrack() = mockk<Track>().also {
                every { it.externalUrls.spotify } returns trackUrl
                every { it.name } returns trackName
                every { it.album } returns simpleAlbum
                every { it.artists } returns listOf(mockk<SimpleArtist>().also {
                    every { it.id  } returns artistId
                    every { it.name } returns artistName
                    every { it.externalUrls.spotify } returns artistUrl
                    coEvery { it.toFullArtist() } returns mockk<Artist>().also {
                        every { it.images } returns emptyList()
                    }
                })
            }

            @Test
            fun `wrong link`() = runTest {
                val testee = SpotifyMessageHandler(mockk<ProgramConfig>(), mockk<SpotifyAppApi>().also {
                    coEvery { it.tracks.getTrack(any()) } returns null
                })
                assertThrows<IllegalArgumentException> {
                    runBlocking {
                        testee.getGenresForTrack("")
                    }
                }
            }

            @Test
            fun `valid input`() {
                val testee = spyk(SpotifyMessageHandler(mockk<ProgramConfig>(), mockk<SpotifyAppApi>().also {
                    coEvery { it.tracks.getTrack(any()) } returns getDefaultTrack()
                })).also {
                    coEvery {it.getGenresForAlbum(any(), any())} returnsArgument(1)
                }
                assertEquals(ResponseData(
                    url = trackUrl,
                    title = trackName,
                    artists = listOf(cc.rbbl.Artist(name = artistName, url = artistUrl))
                ), runBlocking { testee.getGenresForTrack("") })
                coVerify(exactly = 1) { testee.getGenresForAlbum(any(), any()) }
            }
        }
    }
}