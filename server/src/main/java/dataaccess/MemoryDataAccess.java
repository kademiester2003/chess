package dataaccess;

import model.Auth;
import model.Game;
import model.User;
import chess.ChessGame;

import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class MemoryDataAccess implements DataAccess {
    private final HashMap<String, User> users = new HashMap<>();
    private final HashMap<String, Auth> auths = new HashMap<>();
    private final HashMap<Integer, Game> games = new HashMap<>();
    private final AtomicInteger gameIdCounter = new AtomicInteger(1);

    @Override
    public void createUser(User user) throws DataAccessException {
        if (user == null || user.username() == null) throw new DataAccessException("Null user");
        if (users.containsKey(user.username())) throw new DataAccessException("User exists");
        users.put(user.username(), user);
    }

    @Override
    public User getUser(String username) {
        return users.get(username);
    }

    @Override
    public void createAuth(Auth auth) throws DataAccessException {
        if (auth == null || auth.authToken() == null) throw new DataAccessException("Null auth");
        auths.put(auth.authToken(), auth);
    }

    @Override
    public Auth getAuth(String token) {
        return auths.get(token);
    }

    @Override
    public int createGame(Game game) {
        int id = gameIdCounter.getAndIncrement();
        Game stored = new Game(id, game.whiteUsername(), game.blackUsername(), game.gameName(), game.game() == null ? new ChessGame() : game.game());
        games.put(id, stored);
        return id;
    }

    @Override
    public Game getGame(int gameID) {
        return games.get(gameID);
    }

    @Override
    public void deleteAuth(String token) {
        auths.remove(token);
    }

    @Override
    public List<Game> listGames() {
        return new ArrayList<>(games.values());
    }

    @Override
    public void updateGame(Game game) throws DataAccessException {
        if (!games.containsKey(game.gameID())) throw new DataAccessException("Game not found");
        games.put(game.gameID(), game);
    }

    public void clear() {
        users.clear();
        auths.clear();
        games.clear();
        gameIdCounter.set(1);
    }
}
