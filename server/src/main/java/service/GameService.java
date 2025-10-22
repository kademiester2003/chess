package service;

import chess.ChessGame;
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

    public record CreateGamesRequest(String gameName) {}
    public record CreateGamesResult(int gameID) {}


    public CreateGamesResult createGame(String authToken, CreateGamesRequest request) throws DataAccessException{
        if (authToken == null || request == null || request.gameName() == null) throw new IllegalArgumentException("bad request");
        Auth auth = dao.getAuth(authToken);
        if (auth == null) throw new DataAccessException("unauthorized");

        Game toCreate = new Game(0, null, null, request.gameName(), new ChessGame());
        int assignedId = dao.createGame(toCreate);
        return new CreateGamesResult(assignedId);
    }
}
