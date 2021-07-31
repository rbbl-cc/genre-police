package cc.rbbl;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProgramParametersTest {

    private static final String SAMPLE_DISCORD_TOKEN = "NzkzODc2ODExNLM3MTk0Nziy.X-ypZQ.HWYDa7CVJtYDmp4mb4FpKi-I2Vw";
    private static final String SAMPLE_SPOTIFY_CLIENT_ID = "dffB71297e2e4a3f940B2427c3f89a7a";
    private static final String SAMPLE_SPOTIFY_CLIENT_SECRET = "eBbba39cc3155379aef4dd3eb527f3Ba";

    @Nested
    class UncapitalizedTests {
        private final ProgramParameters testee = new ProgramParameters(new String[]{"discordtoken=" + SAMPLE_DISCORD_TOKEN,
                "spotifyclientid=" + SAMPLE_SPOTIFY_CLIENT_ID,
                "spotifyclientsecret=" + SAMPLE_SPOTIFY_CLIENT_SECRET});

        @Test
        void getDiscordToken() {
            assertEquals(SAMPLE_DISCORD_TOKEN, testee.getDiscordToken());
        }

        @Test
        void getSpotifyClientId() {
            assertEquals(SAMPLE_SPOTIFY_CLIENT_ID, testee.getSpotifyClientId());
        }

        @Test
        void getSpotifyClientSecret() {
            assertEquals(SAMPLE_SPOTIFY_CLIENT_SECRET, testee.getSpotifyClientSecret());
        }
    }

    @Nested
    class CapitalizedTests {
        private final ProgramParameters testee = new ProgramParameters(new String[]{"DiscordToken=" + SAMPLE_DISCORD_TOKEN,
                "SpotifyClientID=" + SAMPLE_SPOTIFY_CLIENT_ID,
                "SpotifyClientSecret=" + SAMPLE_SPOTIFY_CLIENT_SECRET});

        @Test
        void getDiscordToken() {
            assertEquals(SAMPLE_DISCORD_TOKEN, testee.getDiscordToken());
        }

        @Test
        void getSpotifyClientId() {
            assertEquals(SAMPLE_SPOTIFY_CLIENT_ID, testee.getSpotifyClientId());
        }

        @Test
        void getSpotifyClientSecret() {
            assertEquals(SAMPLE_SPOTIFY_CLIENT_SECRET, testee.getSpotifyClientSecret());
        }
    }

    @Nested
    class OddCapitalizedTests {
        private final ProgramParameters testee = new ProgramParameters(new String[]{"disCOrdtOken=" + SAMPLE_DISCORD_TOKEN,
                "sPOtifyclIEntid=" + SAMPLE_SPOTIFY_CLIENT_ID,
                "spotIFYclienTSecret=" + SAMPLE_SPOTIFY_CLIENT_SECRET});

        @Test
        void getDiscordToken() {
            assertEquals(SAMPLE_DISCORD_TOKEN, testee.getDiscordToken());
        }

        @Test
        void getSpotifyClientId() {
            assertEquals(SAMPLE_SPOTIFY_CLIENT_ID, testee.getSpotifyClientId());
        }

        @Test
        void getSpotifyClientSecret() {
            assertEquals(SAMPLE_SPOTIFY_CLIENT_SECRET, testee.getSpotifyClientSecret());
        }
    }
}