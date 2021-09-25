package cc.rbbl;

import cc.rbbl.exceptions.NoGenreFoundException;

import java.util.Set;

public interface MessageHandler {
    Set<GenreResponse> getGenreResponses(String message);
}
