package service;

import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import model.*;

import java.util.List;

public class GameService {
    private final DataAccess dao;

    public GameService(DataAccess dao) {
        this.dao = dao;
    }

    public record ListGamesRequest(String authToken) {}
    public record ListGamesResult(List<Game> games) {}

    public ListGamesResult listGames(ListGamesRequest request) throws DataAccessException {
        if (request.authToken() == null) throw new IllegalArgumentException("unauthorized");
        Auth auth = dao.getAuth(request.authToken());
        if (auth == null) throw new IllegalArgumentException("unauthorized");
        var list = dao.listGames();
        return new ListGamesResult(list);
    }
}
