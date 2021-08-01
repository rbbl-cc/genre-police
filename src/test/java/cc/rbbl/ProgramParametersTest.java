package cc.rbbl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProgramParametersTest {

    private static final String SAMPLE_DISCORD_TOKEN = "NzkzODc2ODExNLM3MTk0Nziy.X-ypZQ.HWYDa7CVJtYDmp4mb4FpKi-I2Vw";
    private static final String SAMPLE_SPOTIFY_CLIENT_ID = "dffB71297e2e4a3f940B2427c3f89a7a";
    private static final String SAMPLE_SPOTIFY_CLIENT_SECRET = "eBbba39cc3155379aef4dd3eb527f3Ba";

    private ProgramParameters testee;

    @BeforeEach
    void setUp() {
        testee = new ProgramParameters();
    }

    @Nested
    class UncapitalizedTests {

        @Test
        void getDiscordToken() {
            testee.loadParametersFromArgs(new String[]{"discordtoken=" + SAMPLE_DISCORD_TOKEN});
            assertEquals(SAMPLE_DISCORD_TOKEN, testee.getDiscordToken());
        }

        @Test
        void getSpotifyClientId() {
            testee.loadParametersFromArgs(new String[]{"spotifyclientid=" + SAMPLE_SPOTIFY_CLIENT_ID});
            assertEquals(SAMPLE_SPOTIFY_CLIENT_ID, testee.getSpotifyClientId());
        }

        @Test
        void getSpotifyClientSecret() {
            testee.loadParametersFromArgs(new String[]{"spotifyclientsecret=" + SAMPLE_SPOTIFY_CLIENT_SECRET});
            assertEquals(SAMPLE_SPOTIFY_CLIENT_SECRET, testee.getSpotifyClientSecret());
        }

        @Test
        void loadAllFromArgs() {
            testee.loadParametersFromArgs(new String[]{"discordtoken=" + SAMPLE_DISCORD_TOKEN,
                    "spotifyclientid=" + SAMPLE_SPOTIFY_CLIENT_ID,
                    "spotifyclientsecret=" + SAMPLE_SPOTIFY_CLIENT_SECRET});
            assertEquals(SAMPLE_DISCORD_TOKEN, testee.getDiscordToken());
            assertEquals(SAMPLE_SPOTIFY_CLIENT_ID, testee.getSpotifyClientId());
            assertEquals(SAMPLE_SPOTIFY_CLIENT_SECRET, testee.getSpotifyClientSecret());
        }
    }

    @Nested
    class CapitalizedTests {

        @Test
        void getDiscordToken() {
            testee.loadParametersFromArgs(new String[]{"DiscordToken=" + SAMPLE_DISCORD_TOKEN});
            assertEquals(SAMPLE_DISCORD_TOKEN, testee.getDiscordToken());
        }

        @Test
        void getSpotifyClientId() {
            testee.loadParametersFromArgs(new String[]{"SpotifyClientID=" + SAMPLE_SPOTIFY_CLIENT_ID});
            assertEquals(SAMPLE_SPOTIFY_CLIENT_ID, testee.getSpotifyClientId());
        }

        @Test
        void getSpotifyClientSecret() {
            testee.loadParametersFromArgs(new String[]{"SpotifyClientSecret=" + SAMPLE_SPOTIFY_CLIENT_SECRET});
            assertEquals(SAMPLE_SPOTIFY_CLIENT_SECRET, testee.getSpotifyClientSecret());
        }

        @Test
        void loadAllFromArgs() {
            testee.loadParametersFromArgs(new String[]{"DiscordToken=" + SAMPLE_DISCORD_TOKEN,
                    "SpotifyClientID=" + SAMPLE_SPOTIFY_CLIENT_ID,
                    "SpotifyClientSecret=" + SAMPLE_SPOTIFY_CLIENT_SECRET});
            assertEquals(SAMPLE_DISCORD_TOKEN, testee.getDiscordToken());
            assertEquals(SAMPLE_SPOTIFY_CLIENT_ID, testee.getSpotifyClientId());
            assertEquals(SAMPLE_SPOTIFY_CLIENT_SECRET, testee.getSpotifyClientSecret());
        }
    }

    @Nested
    class OddCapitalizedTests {

        @Test
        void loadDiscordTokenFromArgs() {
            testee.loadParametersFromArgs(new String[]{"disCOrdtOken=" + SAMPLE_DISCORD_TOKEN});
            assertEquals(SAMPLE_DISCORD_TOKEN, testee.getDiscordToken());
        }

        @Test
        void loadSpotifyClientIdFromArgs() {
            testee.loadParametersFromArgs(new String[]{"sPOtifyclIEntid=" + SAMPLE_SPOTIFY_CLIENT_ID});
            assertEquals(SAMPLE_SPOTIFY_CLIENT_ID, testee.getSpotifyClientId());
        }

        @Test
        void loadSpotifyClientSecretFromArgs() {
            testee.loadParametersFromArgs(new String[]{"spotIFYclienTSecret=" + SAMPLE_SPOTIFY_CLIENT_SECRET});
            assertEquals(SAMPLE_SPOTIFY_CLIENT_SECRET, testee.getSpotifyClientSecret());
        }

        @Test
        void loadAllFromArgs() {
            testee.loadParametersFromArgs(new String[]{"disCOrdtOken=" + SAMPLE_DISCORD_TOKEN,
                    "sPOtifyclIEntid=" + SAMPLE_SPOTIFY_CLIENT_ID,
                    "spotIFYclienTSecret=" + SAMPLE_SPOTIFY_CLIENT_SECRET});
            assertEquals(SAMPLE_DISCORD_TOKEN, testee.getDiscordToken());
            assertEquals(SAMPLE_SPOTIFY_CLIENT_ID, testee.getSpotifyClientId());
            assertEquals(SAMPLE_SPOTIFY_CLIENT_SECRET, testee.getSpotifyClientSecret());
        }
    }

    @Test
    void loadFromEmptyArgs() {
        assertDoesNotThrow(() -> {
            testee.loadParametersFromArgs(new String[]{});
        });
    }

    @Test
    void loadFromNullArgs() {
        assertDoesNotThrow(() -> {
            testee.loadParametersFromArgs(null);
        });
    }

    @Test
    void checkIfParamsCompletePositive() {
        testee.loadParametersFromArgs(new String[]{"DiscordToken=" + SAMPLE_DISCORD_TOKEN,
                "SpotifyClientID=" + SAMPLE_SPOTIFY_CLIENT_ID,
                "SpotifyClientSecret=" + SAMPLE_SPOTIFY_CLIENT_SECRET});
        assertDoesNotThrow(() -> {
            testee.checkIfParamsComplete();
        });
    }

    @Test
    void checkIfParamsCompleteNegativeSingle() {
        testee.loadParametersFromArgs(new String[]{"DiscordToken=" + SAMPLE_DISCORD_TOKEN,
                "SpotifyClientID=" + SAMPLE_SPOTIFY_CLIENT_ID});
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> {
            testee.checkIfParamsComplete();
        });
        assertEquals("Missing required Parameters: SpotifyClientSecret", illegalArgumentException.getMessage());
    }

    @Test
    void checkIfParamsCompleteNegativeAll() {
        testee.loadParametersFromArgs(new String[]{});
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> {
            testee.checkIfParamsComplete();
        });
        assertEquals("Missing required Parameters: DiscordToken SpotifyClientId SpotifyClientSecret", illegalArgumentException.getMessage());
    }
}