package dataaccess;

import chess.ChessGame;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import model.Auth;
import model.Game;
import model.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MySQLDataAccess implements DataAccess {

    private final Gson gson = new Gson();

    public MySQLDataAccess() throws DataAccessException {
        configureDatabase();
    }

    public void configureDatabase() throws DataAccessException {
        try (var conn = DatabaseManager.getConnection()) {
            try (var stmt = conn.createStatement()) {
                // Example users table
                stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS users (
                    username VARCHAR(50) PRIMARY KEY,
                    passwordHash VARCHAR(255) NOT NULL,
                    email VARCHAR(255)
                )
            """);

                // Example games table
                stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS games (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    gameName VARCHAR(100) NOT NULL,
                    whiteUsername VARCHAR(50),
                    blackUsername VARCHAR(50),
                    gameState JSON
                )
            """);

                // Example auth tokens table
                stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS auth (
                    authToken VARCHAR(100) PRIMARY KEY,
                    username VARCHAR(50) NOT NULL
                )
            """);
            }
        } catch (SQLException ex) {
            throw new DataAccessException("Failed to configure database", ex);
        }
    }

    @Override
    public void clear() throws DataAccessException {
        try (var conn = DatabaseManager.getConnection();
             var statement = conn.createStatement()) {
            statement.executeUpdate("DELETE FROM Auths");
            statement.executeUpdate("DELETE FROM Games");
            statement.executeUpdate("DELETE FROM Users");
        } catch (SQLException ex) {
            throw new DataAccessException("failed to clear database", ex);
        }
    }

    @Override
    public void createUser(User user) throws DataAccessException {
        if (user == null) {
            throw new DataAccessException("null user");
        }
        final String sql = "INSERT INTO Users (username, password, email) VALUES (?, ?, ?)";
        try (var conn = DatabaseManager.getConnection();
             var ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.username());
            ps.setString(2, user.password());
            ps.setString(3, user.email());
            ps.executeUpdate();
        } catch (SQLException ex) {
            if (ex.getSQLState() != null && (ex.getSQLState().startsWith("23") || ex.getErrorCode() == 1062)) {
                throw new DataAccessException("user exists", ex);
            }
            throw new DataAccessException("failed to create user", ex);
        }
    }

    @Override
    public User getUser(String username) throws DataAccessException {
        final String sql = "SELECT username, password, email FROM users WHERE username = ?";
        try (var conn = DatabaseManager.getConnection();
             var ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (var result = ps.executeQuery()) {
                if (!result.next()) {
                    return null;
                }
                return new User(result.getString("username"),
                                result.getString("password"),
                                result.getString("email"));
            }
        } catch (SQLException ex) {
            throw new DataAccessException("failed to get user", ex);
        }
    }

    public void createAuth(Auth auth) throws DataAccessException {
        final String sql = "INSERT INTO Auths (authToken, username) VALUES (?, ?)";
        try (var conn = DatabaseManager.getConnection();
             var ps = conn.prepareStatement(sql)) {
            ps.setString(1, auth.authToken());
            ps.setString(2, auth.username());
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new DataAccessException("failed to create auth", ex);
        }
    }

    @Override
    public Auth getAuth(String authToken) throws DataAccessException {
        final String sql = "SELECT authToken, username FROM Auths WHERE authToken = ?";
        try (var conn = DatabaseManager.getConnection();
             var ps = conn.prepareStatement(sql)) {
            ps.setString(1, authToken);
            try (var result = ps.executeQuery()) {
                if (!result.next()) {
                    return null;
                }
                return new Auth(result.getString("authToken"), result.getString("username"));
            }
        } catch (SQLException ex) {
            throw new DataAccessException("failed to get auth", ex);
        }
    }

    @Override
    public void deleteAuth(String authToken) throws DataAccessException {
        final String sql = "DELETE FROM Auths WHERE authToken = ?";
        try (var conn = DatabaseManager.getConnection();
             var ps = conn.prepareStatement(sql)) {
            ps.setString(1, authToken);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new DataAccessException("failed to delete auth", ex);
        }
    }

    @Override
    public int createGame(Game game) throws DataAccessException {
        final String sql = "INSERT INTO Games (whiteUsername, blackUsername, gameName, game) VALUES (?, ?, ?, ?)";
        String json = null;
        if (game.game() != null) {
            json = gson.toJson(game.game());
        }
        try (var conn = DatabaseManager.getConnection();
             var ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            putDataInSQL(game, json, ps);

            ps.executeUpdate();
            try (var result = ps.getGeneratedKeys()) {
                if (result.next()) {
                    return result.getInt(1);
                } else {
                    throw new DataAccessException("failed to retrieve game ID");
                }
            }
        } catch (SQLException ex) {
            throw new DataAccessException("failed to create game", ex);
        }
    }

    private void putDataInSQL(Game game, String json, PreparedStatement ps) throws SQLException {
        if (game.whiteUsername() != null) {
            ps.setString(1, game.whiteUsername());
        } else {
            ps.setNull(1, Types.VARCHAR);
        }
        if (game.blackUsername() != null) {
            ps.setString(2, game.blackUsername());
        } else {
            ps.setNull(2, Types.VARCHAR);
        }
        ps.setString(3, game.gameName());
        if (json != null) {
            ps.setString(4, json);
        } else {
            ps.setNull(4, Types.LONGNVARCHAR);
        }
    }

    @Override
    public Game getGame(int gameID) throws DataAccessException {
        final String sql = "SELECT gameID, whiteUsername, blackUsername, gameName, game FROM Games WHERE gameID = ?";
        try (var conn = DatabaseManager.getConnection();
             var ps = conn.prepareStatement(sql)) {
            ps.setInt(1, gameID);
            try (var result = ps.executeQuery()) {
                if (!result.next()) {
                    return null;
                }
                ChessGame chessGame = null;
                String json = result.getString("game");
                if (json != null) {
                    try {
                        chessGame = gson.fromJson(json, ChessGame.class);
                    } catch (JsonSyntaxException ex) {}
                }
                return new Game(result.getInt("gameID"),
                                result.getString("whiteUsername"),
                                result.getString("blackUsername"),
                                result.getString("gameName"),
                                chessGame);
            }
        } catch (SQLException ex) {
            throw new DataAccessException("failed to get game", ex);
        }
    }

    @Override
    public List<Game> listGames() throws DataAccessException {
        final String sql = "SELECT gameID, whiteUsername, blackUsername, gameName, game FROM Games";
        List<Game> games = new ArrayList<>();
        try (var conn = DatabaseManager.getConnection();
             var ps = conn.prepareStatement(sql);
             var result = ps.executeQuery()) {

            while (result.next()) {
                ChessGame chessGame = null;
                String json = result.getString("game");
                if (json != null) {
                    try {
                        chessGame = gson.fromJson(json, ChessGame.class);
                    } catch (JsonSyntaxException ex) {}
                }
                games.add(new Game(result.getInt("gameID"),
                                    result.getString("whiteUsername"),
                                    result.getString("blackUsername"),
                                    result.getString("gameName"),
                                    chessGame));
            }
            return games;
        } catch (SQLException ex) {
            throw new DataAccessException("failed to get game", ex);
        }
    }

    @Override
    public void updateGame(Game game) throws DataAccessException {
        final String sql = "UPDATE Games SET whiteUsername = ?, blackUsername = ?, gameName = ?, game = ? " +
                           "WHERE gameID = ?";
        String json = null;
        if (game.game() != null) {
            json = gson.toJson(game.game());
        }
        try (var conn = DatabaseManager.getConnection();
             var ps = conn.prepareStatement(sql)) {
            putDataInSQL(game, json, ps);
            ps.setInt(5, game.gameID());

            int affected = ps.executeUpdate();
            if (affected == 0) {
                throw new DataAccessException("game not found");
            }
        } catch (SQLException ex) {
            throw new DataAccessException("failed to update game", ex);
        }
    }
}
