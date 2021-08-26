package cc.rbbl;

import cc.rbbl.exceptions.NoGenreFoundException;
import cc.rbbl.program_parameters_jvm.ParameterHolder;
import com.wrapper.spotify.SpotifyApi;
import com.wrapper.spotify.exceptions.SpotifyWebApiException;
import com.wrapper.spotify.exceptions.detailed.UnauthorizedException;
import com.wrapper.spotify.model_objects.credentials.ClientCredentials;
import com.wrapper.spotify.model_objects.specification.Album;
import com.wrapper.spotify.model_objects.specification.Artist;
import com.wrapper.spotify.model_objects.specification.ArtistSimplified;
import com.wrapper.spotify.model_objects.specification.Track;
import org.apache.hc.core5.http.ParseException;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SpotifyLinkHandler {
    private final static String SPOTIFY_LINK_PATTERN = "\\bhttps://open.spotify.com/(?:track|album|artist)[^ \\t\\n\\r]*\\b";

    private static final String spotifyDomain = "https://open.spotify.com/";
    private final SpotifyApi spotifyApi;
    private int retryCounter;

    public SpotifyLinkHandler(ParameterHolder parameters) throws ParseException, SpotifyWebApiException, IOException {
        spotifyApi = new SpotifyApi.Builder().setClientId(parameters.get("SpotifyClientID"))
                .setClientSecret(parameters.get("SpotifyClientSecret")).build();
        ClientCredentials clientCredentials = spotifyApi.clientCredentials().build().execute();
        spotifyApi.setAccessToken(clientCredentials.getAccessToken());
    }

    public String[] getGenres(String message) throws NoGenreFoundException {
        try {
            Matcher matcher = Pattern.compile(SPOTIFY_LINK_PATTERN).matcher(message);
            if (matcher.find() && !message.toLowerCase().contains("genre")) {
                String typeSlashId = matcher.group().split("\\?")[0].replace(spotifyDomain, "");
                switch (typeSlashId.split("/")[0].toLowerCase()) {
                    case "track":
                        return getGenresForTrack(typeSlashId.split("/")[1]);
                    case "album":
                        return getGenresForAlbum(typeSlashId.split("/")[1]);
                    case "artist":
                        return getGenresForArtist(typeSlashId.split("/")[1]);
                }
            }
        } catch (UnauthorizedException e) {
            if (retryCounter < 3) {
                retryCounter++;
                refreshSpotifyToken();
                return getGenres(message);
            }
        }
        return null;
    }

    private String[] getGenresForTrack(String trackId) throws UnauthorizedException, NoGenreFoundException {
        try {
            Track track = spotifyApi.getTrack(trackId).build().execute();
            return getGenresForAlbum(track.getAlbum().getId());
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SpotifyWebApiException e) {
            if (e instanceof UnauthorizedException) {
                throw (UnauthorizedException) e;
            } else {
                e.printStackTrace();
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        throw new NoGenreFoundException();
    }

    private String[] getGenresForAlbum(String albumId) throws UnauthorizedException, NoGenreFoundException {
        try {
            Album album = spotifyApi.getAlbum(albumId).build().execute();
            if (album.getGenres().length != 0) {
                return album.getGenres();
            } else {
                for (ArtistSimplified artistSimplified : album.getArtists()) {
                    String[] genres = getGenresForArtist(artistSimplified.getId());
                    if (genres != null) {
                        return genres;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SpotifyWebApiException e) {
            if (e instanceof UnauthorizedException) {
                throw (UnauthorizedException) e;
            } else {
                e.printStackTrace();
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        throw new NoGenreFoundException();
    }

    private String[] getGenresForArtist(String artistId) throws UnauthorizedException, NoGenreFoundException {
        try {
            Artist artist = spotifyApi.getArtist(artistId).build().execute();
            if (artist.getGenres().length != 0) {
                return artist.getGenres();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SpotifyWebApiException e) {
            if (e instanceof UnauthorizedException) {
                throw (UnauthorizedException) e;
            } else {
                e.printStackTrace();
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        throw new NoGenreFoundException();
    }

    private void refreshSpotifyToken() {
        try {
            ClientCredentials clientCredentials = spotifyApi.clientCredentials().build().execute();
            spotifyApi.setAccessToken(clientCredentials.getAccessToken());
            retryCounter = 0;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SpotifyWebApiException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }
}
