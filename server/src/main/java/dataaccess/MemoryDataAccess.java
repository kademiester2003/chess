package dataaccess;

import model.Auth;
import model.Game;
import model.User;

import java.util.HashMap;
import java.util.List;

public class MemoryDataAccess implements DataAccess {
    private final HashMap<String, User> users = new HashMap<>();
    private final HashMap<String, Auth> auths = new HashMap<>();

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
    public Auth getAuth(String token) throws DataAccessException {
        return auths.get(token);
    }

    @Override
    public void deleteAuth(String token) throws DataAccessException {
        auths.remove(token);
    }

    @Override
    public int createGame(Game game) throws DataAccessException {
        return 0;
    }

    @Override
    public Game getGame(int gameID) throws DataAccessException {
        return null;
    }

    @Override
    public List<Game> getGames() throws DataAccessException {
        return List.of();
    }

    @Override
    public void updateGame(Game game) throws DataAccessException {

    }
}
