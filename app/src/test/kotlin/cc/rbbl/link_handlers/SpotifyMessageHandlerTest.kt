package cc.rbbl.link_handlers

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class SpotifyMessageHandlerTest {
    @Test
    fun `regular track url`() {
        assertTrue {
            SpotifyMessageHandler.SPOTIFY_LINK_PATTERN.matches("https://open.spotify.com/track/2p4p9YGwmJIdf5IA9sSWhm?si=ac6b3105df714569")
        }
    }

    @Test
    fun `regular anonymous track url`() {
        assertTrue {
            SpotifyMessageHandler.SPOTIFY_LINK_PATTERN.matches("https://open.spotify.com/track/2p4p9YGwmJIdf5IA9sSWhm")
        }
    }

    @Test
    fun `regular album url`() {
        assertTrue {
            SpotifyMessageHandler.SPOTIFY_LINK_PATTERN.matches("https://open.spotify.com/album/5WS1g0cKtjfK6eDoSLdv7d?si=ff8085a125f24d7d")
        }
    }

    @Test
    fun `regular anonymous album url`() {
        assertTrue {
            SpotifyMessageHandler.SPOTIFY_LINK_PATTERN.matches("https://open.spotify.com/album/5WS1g0cKtjfK6eDoSLdv7d")
        }
    }

    @Test
    fun `regular artist url`() {
        assertTrue {
            SpotifyMessageHandler.SPOTIFY_LINK_PATTERN.matches("https://open.spotify.com/artist/2o8lOQRjzsSC8UdbNN88HN?si=F970p5vnSpeFWeYz1uFpwQ")
        }
    }

    @Test
    fun `regular anonymous artist url`() {
        assertTrue {
            SpotifyMessageHandler.SPOTIFY_LINK_PATTERN.matches("https://open.spotify.com/artist/2o8lOQRjzsSC8UdbNN88HN")
        }
    }

    @Test
    fun `regular episode url`() {
        assertFalse {
            SpotifyMessageHandler.SPOTIFY_LINK_PATTERN.matches("https://open.spotify.com/episode/0UJirr3XsKjh2VI18aM6Bj?si=f918ba9433f041f6")
        }
    }

    @Test
    fun `regular anonymous episode url`() {
        assertFalse {
            SpotifyMessageHandler.SPOTIFY_LINK_PATTERN.matches("https://open.spotify.com/episode/0UJirr3XsKjh2VI18aM6Bj")
        }
    }

    @Test
    fun `regular show url`() {
        assertFalse {
            SpotifyMessageHandler.SPOTIFY_LINK_PATTERN.matches("https://open.spotify.com/show/6KnaAHvqf0pgTs3Kw3qQTR?si=cad36accf40245b3")
        }
    }

    @Test
    fun `regular anonymous show url`() {
        assertFalse {
            SpotifyMessageHandler.SPOTIFY_LINK_PATTERN.matches("https://open.spotify.com/show/6KnaAHvqf0pgTs3Kw3qQTR")
        }
    }

    @Test
    fun `market track url`() {
        assertTrue {
            SpotifyMessageHandler.SPOTIFY_LINK_PATTERN.matches("https://open.spotify.com/intl-de/track/4awF9g7FMdeTxeD4OaSUIa?si=e6d6d7cc272c4873")
        }
    }

    @Test
    fun `market anonymous track url`() {
        assertTrue {
            SpotifyMessageHandler.SPOTIFY_LINK_PATTERN.matches("https://open.spotify.com/intl-de/track/4awF9g7FMdeTxeD4OaSUIa")
        }
    }

    @Test
    fun `short track url`() {
        assertTrue {
            SpotifyMessageHandler.SPOTIFY_LINK_PATTERN.matches("https://spotify.link/elxIFeXj2Ab")
        }
    }

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

    @Test
    fun `resolve short url`() = runTest {
        assertContentEquals(
            arrayOf("track", "39iRz0h1eZOyXzch8tKQit"),
            SpotifyMessageHandler.extractTypeAndId("https://spotify.link/elxIFeXj2Ab")
        )
    }
}