package dataaccess;

import model.*;

import java.util.List;

public interface DataAccess {
    void createUser(User user) throws DataAccessException;
    User getUser(String username) throws DataAccessException;

    void createAuth(Auth auth) throws DataAccessException;
    Auth getAuth(String token) throws DataAccessException;
    void deleteAuth(String token) throws DataAccessException;

    int createGame(Game game) throws DataAccessException;
    Game getGame(int gameID) throws DataAccessException;
    List<Game> getGames() throws DataAccessException;
    void updateGame(Game game) throws DataAccessException;
}
