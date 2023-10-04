package cc.rbbl

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import org.junit.jupiter.api.Nested
import kotlin.test.assertNull


internal class ResponseDataTest {

    @Nested
    inner class ResponseDataTest {
        @Test
        fun `getDescription without artists`() {
            assertNull(ResponseData().getDescription())
        }

        @Test
        fun `getDescription with empty artists`() {
            assertNull(ResponseData(artists = emptyList()).getDescription())
        }

        @Test
        fun `getDescription with a single artists`() {
            assertNull(ResponseData(artists = listOf(Artist("Lmao", "gottem"))).getDescription())
        }

        @Test
        fun `getDescription with multiple artists`() {
            assertEquals("feat. [Lmao](gottem)", ResponseData(artists = listOf(Artist("ignoredCauseFirst"), Artist("Lmao", "gottem"))).getDescription())
        }
    }

    @Nested
    inner class ArtistTest{
        @Test
        fun `with URL`() {
            assertEquals("[Name](URL)", Artist("Name", "URL").toMarkdown())
        }

        @Test
        fun `without URL`() {
            assertEquals("Name", Artist("Name").toMarkdown())
        }
    }
}