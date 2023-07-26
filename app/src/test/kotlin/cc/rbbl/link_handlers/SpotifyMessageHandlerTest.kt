package cc.rbbl.link_handlers

import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class SpotifyMessageHandlerTest {
    @Test
    fun `regular track link`() {
        assertTrue {
            SpotifyMessageHandler.SPOTIFY_LINK_PATTERN.matches("https://open.spotify.com/track/2p4p9YGwmJIdf5IA9sSWhm?si=ac6b3105df714569")
        }
    }

    @Test
    fun `regular anonymous track link`() {
        assertTrue {
            SpotifyMessageHandler.SPOTIFY_LINK_PATTERN.matches("https://open.spotify.com/track/2p4p9YGwmJIdf5IA9sSWhm")
        }
    }

    @Test
    fun `regular album link`() {
        assertTrue {
            SpotifyMessageHandler.SPOTIFY_LINK_PATTERN.matches("https://open.spotify.com/album/5WS1g0cKtjfK6eDoSLdv7d?si=ff8085a125f24d7d")
        }
    }

    @Test
    fun `regular anonymous album link`() {
        assertTrue {
            SpotifyMessageHandler.SPOTIFY_LINK_PATTERN.matches("https://open.spotify.com/album/5WS1g0cKtjfK6eDoSLdv7d")
        }
    }

    @Test
    fun `regular artist link`() {
        assertTrue {
            SpotifyMessageHandler.SPOTIFY_LINK_PATTERN.matches("https://open.spotify.com/artist/2o8lOQRjzsSC8UdbNN88HN?si=F970p5vnSpeFWeYz1uFpwQ")
        }
    }

    @Test
    fun `regular anonymous artist link`() {
        assertTrue {
            SpotifyMessageHandler.SPOTIFY_LINK_PATTERN.matches("https://open.spotify.com/artist/2o8lOQRjzsSC8UdbNN88HN")
        }
    }

    @Test
    fun `regular episode link`() {
        assertFalse {
            SpotifyMessageHandler.SPOTIFY_LINK_PATTERN.matches("https://open.spotify.com/episode/0UJirr3XsKjh2VI18aM6Bj?si=f918ba9433f041f6")
        }
    }

    @Test
    fun `regular anonymous episode link`() {
        assertFalse {
            SpotifyMessageHandler.SPOTIFY_LINK_PATTERN.matches("https://open.spotify.com/episode/0UJirr3XsKjh2VI18aM6Bj")
        }
    }

    @Test
    fun `regular show link`() {
        assertFalse {
            SpotifyMessageHandler.SPOTIFY_LINK_PATTERN.matches("https://open.spotify.com/show/6KnaAHvqf0pgTs3Kw3qQTR?si=cad36accf40245b3")
        }
    }

    @Test
    fun `regular anonymous show link`() {
        assertFalse {
            SpotifyMessageHandler.SPOTIFY_LINK_PATTERN.matches("https://open.spotify.com/show/6KnaAHvqf0pgTs3Kw3qQTR")
        }
    }
}