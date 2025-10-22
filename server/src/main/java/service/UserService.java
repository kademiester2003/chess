package service;

import dataaccess.DataAccess;
import model.RegistrationResult;
import model.LoginResult;
import model.ListGamesResult;
import model.User;
import java.util.UUID;

public class UserService {
    private final DataAccess dataAccess;
    public UserService(DataAccess dataAccess) {
        this.dataAccess = dataAccess;
    }

    public RegistrationResult register(User user) {
        dataAccess.saveUser(user);
        String authToken = UUID.randomUUID().toString();
        return new RegistrationResult(user.username(), authToken);
    }

    public LoginResult login(User user) {
        return null;
    }

    public ListGamesResult listGames(User user) {
    }

    public Object createGame(User req) {
    }

    public Object joinGame(User req) {
    }
}
