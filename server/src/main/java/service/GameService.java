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

    public record ListGamesResult(List<Game> games) {}

    public ListGamesResult listGames(String authToken) throws DataAccessException {
        if (authToken == null) {
            throw new IllegalArgumentException("bad request");
        }
        Auth auth = dao.getAuth(authToken);
        if (auth == null) {
            throw new IllegalArgumentException("unauthorized");
        }
        var list = dao.listGames();
        return new ListGamesResult(list);
    }

    public record CreateGameRequest(String gameName) {}
    public record CreateGameResult(int gameID) {}


    public CreateGameResult createGame(String authToken, CreateGameRequest request) throws DataAccessException{
        if (authToken == null || request == null || request.gameName() == null) {
            throw new IllegalArgumentException("bad request"); //
        }
        Auth auth = dao.getAuth(authToken);
        if (auth == null) {
            throw new DataAccessException("unauthorized");
        }

        Game toCreate = new Game(0, null, null, request.gameName(), new ChessGame());
        int assignedId = dao.createGame(toCreate);
        return new CreateGameResult(assignedId);
    }

    public record JoinGameRequest(String playerColor, int gameID) {}

    public void joinGame(String authToken, JoinGameRequest request) throws DataAccessException {
        if (authToken == null || request == null) {
            throw new IllegalArgumentException("bad request"); //
        }
        Auth auth = dao.getAuth(authToken);
        if  (auth == null) {
            throw new DataAccessException("unauthorized");
        }

        Game game = dao.getGame(request.gameID());
        if (game == null) {
            throw new DataAccessException("game not found");
        }

        String player = auth.username();
        String color = request.playerColor();
        if (!"WHITE".equals(color) && !"BLACK".equals(color)) {
            throw new IllegalArgumentException("bad request"); //
        }

        if ("WHITE".equals(color)) {
            if (game.whiteUsername() != null) {
                throw new DataAccessException("already taken");
            }
            Game updated = new Game(game.gameID(), player, game.blackUsername(), game.gameName(), game.game());
            dao.updateGame(updated);
        } else {
            if (game.blackUsername() != null) {
                throw new DataAccessException("already taken");
            }
            Game updated = new Game(game.gameID(), game.whiteUsername(), player, game.gameName(), game.game());
            dao.updateGame(updated);
        }
    }

}
