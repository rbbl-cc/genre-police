package cc.rbbl;

public class ProgramParameters {

    private String discordToken;
    private String spotifyClientId;
    private String spotifyClientSecret;

    public ProgramParameters() {

    }

    public ProgramParameters(String[] args) {
        loadParametersFromArgs(args);
    }

    public void loadParametersFromArgs(String[] args) {
        if (args == null) {
            return;
        }
        for (String arg : args) {
            String[] keyValue = arg.split("=");
            if (keyValue.length != 2) {
                continue;
            }

            switch (keyValue[0].toLowerCase()) {
                case "discordtoken":
                    discordToken = keyValue[1];
                    break;
                case "spotifyclientid":
                    spotifyClientId = keyValue[1];
                    break;
                case "spotifyclientsecret":
                    spotifyClientSecret = keyValue[1];
                    break;
            }
        }
    }

    /***
     *
     * @throws IllegalArgumentException when any required parameters are missing
     */
    public void checkIfParamsComplete() throws IllegalArgumentException {
        boolean discordTokenMissing = this.discordToken == null;
        boolean spotifyClientIdMissing = this.spotifyClientId == null;
        boolean spotifyClientSecretMissing = this.spotifyClientSecret == null;

        if (discordTokenMissing || spotifyClientIdMissing || spotifyClientSecretMissing) {
            String errorMessage = "Missing required Parameters:";
            if (discordTokenMissing) {
                errorMessage += " DiscordToken";
            }
            if (spotifyClientIdMissing) {
                errorMessage += " SpotifyClientId";
            }
            if (spotifyClientSecretMissing) {
                errorMessage += " SpotifyClientSecret";
            }
            throw new IllegalArgumentException(errorMessage);
        }
    }

    public String getDiscordToken() {
        return discordToken;
    }

    public String getSpotifyClientId() {
        return spotifyClientId;
    }

    public String getSpotifyClientSecret() {
        return spotifyClientSecret;
    }
}
