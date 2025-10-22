package service;

import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import model.*;

import java.util.UUID;

public class UserService {
    private final DataAccess dao;

    public UserService(DataAccess dao) {
        this.dao = dao;
    }

    public record RegisterRequest(String username, String password, String email) {}
    public record RegisterResult(String username, String authToken) {}

    public RegisterResult register(RegisterRequest request) throws DataAccessException, IllegalArgumentException {
        if (request == null || request.username() == null || request.password() == null || request.email() == null) {
            throw new IllegalArgumentException("bad request");
        }

        var existing = dao.getUser(request.username());
        if (existing != null) throw new DataAccessException("already taken");

        User user = new User(request.username(), request.password(), request.email());
        dao.createUser(user);

        String token = generateToken();
        dao.createAuth(new Auth(token, request.username()));
        return new RegisterResult(request.username(), token);
    }

    public static String generateToken() {
        return UUID.randomUUID().toString();
    }

    public record LoginRequest(String username, String password) {}
    public record LoginResult(String username, String authToken) {}

    public LoginResult login(LoginRequest request) throws DataAccessException, IllegalArgumentException {
        if (request == null || request.username == null || request.password == null) {
            throw new IllegalArgumentException("bad request");
        }

        User user = dao.getUser(request.username);
        if (user == null) throw new DataAccessException("unauthorized");
        if (!user.password().equals(request.password)) throw new DataAccessException("unauthorized");

        String token = generateToken();
        dao.createAuth(new Auth(token, user.username()));
        return new LoginResult(request.username, token);
    }

    public void logout(String authToken) throws DataAccessException, IllegalArgumentException {
        if (authToken == null) throw new IllegalArgumentException("unauthorized");
        var auth = dao.getAuth(authToken);
        if (auth == null) throw new DataAccessException("unauthorized");
        dao.deleteAuth(authToken);
    }

}
