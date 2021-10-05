package cc.rbbl.link_handlers;

import cc.rbbl.GenreResponse;
import cc.rbbl.MessageHandler;
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
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SpotifyMessageHandler implements MessageHandler {
    private final static String SPOTIFY_LINK_PATTERN = "\\bhttps://open.spotify.com/(?:track|album|artist)[^ \\t\\n\\r]*\\b";

    private static final String spotifyDomain = "https://open.spotify.com/";
    private final SpotifyApi spotifyApi;
    private int retryCounter;

    public SpotifyMessageHandler(ParameterHolder parameters) throws ParseException, SpotifyWebApiException, IOException {
        spotifyApi = new SpotifyApi.Builder().setClientId(parameters.get("SPOTIFY_CLIENT_ID"))
                .setClientSecret(parameters.get("SPOTIFY_CLIENT_SECRET")).build();
        ClientCredentials clientCredentials = spotifyApi.clientCredentials().build().execute();
        spotifyApi.setAccessToken(clientCredentials.getAccessToken());
    }

    @Override
    public Set<GenreResponse> getGenreResponses(String message) {
        try {
            HashSet<GenreResponse> results = new HashSet<>();
            Matcher matcher = Pattern.compile(SPOTIFY_LINK_PATTERN).matcher(message);
            if (!message.toLowerCase().contains("genre")) {
                while (matcher.find()) {
                    try {
                        String typeSlashId = matcher.group().split("\\?")[0].replace(spotifyDomain, "");
                        switch (typeSlashId.split("/")[0].toLowerCase()) {
                            case "track":
                                results.add(getGenresForTrack(typeSlashId.split("/")[1]));
                                break;
                            case "album":
                                results.add(getGenresForAlbum(typeSlashId.split("/")[1], null));
                                break;
                            case "artist":
                                results.add(getGenresForArtist(typeSlashId.split("/")[1], null));
                                break;
                        }
                    } catch (NoGenreFoundException e) {
                        results.add(new GenreResponse(e.getItemName(), "Spotify has no genre for that Item"));
                    }
                }
            }
            return results;
        } catch (UnauthorizedException e) {
            if (retryCounter < 3) {
                retryCounter++;
                refreshSpotifyToken();
                return getGenreResponses(message);
            }
        }
        throw new UnknownError();
    }

    private GenreResponse getGenresForTrack(String trackId) throws UnauthorizedException, NoGenreFoundException {
        try {
            Track track = spotifyApi.getTrack(trackId).build().execute();
            return getGenresForAlbum(track.getAlbum().getId(), track.getName());
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
        return null;
    }

    private GenreResponse getGenresForAlbum(String albumId, String title) throws UnauthorizedException, NoGenreFoundException {
        try {
            Album album = spotifyApi.getAlbum(albumId).build().execute();
            if (album.getGenres().length != 0) {
                return new GenreResponse(title, genresToMessage(album.getGenres()));
            } else {
                for (ArtistSimplified artistSimplified : album.getArtists()) {
                    return getGenresForArtist(artistSimplified.getId(), title == null ? album.getName() : title);
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
        throw new UnknownError();
    }

    private GenreResponse getGenresForArtist(String artistId, String title) throws UnauthorizedException, NoGenreFoundException {
        try {
            Artist artist = spotifyApi.getArtist(artistId).build().execute();
            if (artist.getGenres().length != 0) {
                return new GenreResponse(title == null ? artist.getName() : title, genresToMessage(artist.getGenres()));
            }
            throw new NoGenreFoundException(title == null ? artist.getName() : title);
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
        throw new UnknownError();
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

    private String genresToMessage(String[] genres) {
        if (genres == null || genres.length == 0) {
            return null;
        }
        StringBuilder message = new StringBuilder();
        for (String genre : genres) {
            message.append("\"").append(genre).append("\" ");
        }
        return message.toString();
    }
}
