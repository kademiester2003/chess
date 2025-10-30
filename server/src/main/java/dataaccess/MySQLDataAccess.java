package dataaccess;

import com.google.gson.Gson;
import model.User;

import java.sql.SQLException;

public class MySQLDataAccess implements DataAccess {

    private final Gson gson = new Gson();

    public MySQLDataAccess() {}

    public void createTablesIfNotExist() throws DataAccessException {
        final String usersSql = """
                CREATE TABLE IF NOT EXISTS users (
                  username VARCHAR(100) NOT NULL PRIMARY KEY,
                  password VARCHAR(255) NOT NULL,
                  email VARCHAR(255) NOT NULL
                )
                """;

        final String authsSql = """
            CREATE TABLE IF NOT EXISTS Auths (
              authToken VARCHAR(255) NOT NULL PRIMARY KEY,
              username VARCHAR(100) NOT NULL,
              FOREIGN KEY (username) REFERENCES Users(username) ON DELETE CASCADE
            )
            """;

        final String gamesSql = """
            CREATE TABLE IF NOT EXISTS Games (
              gameID INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
              whiteUsername VARCHAR(100),
              blackUsername VARCHAR(100),
              gameName VARCHAR(255) NOT NULL,
              game TEXT,
              FOREIGN KEY (whiteUsername) REFERENCES Users(username),
              FOREIGN KEY (blackUsername) REFERENCES Users(username)
            )
            """;

        try (var conn = DatabaseManager.getConnection()) {
            try (var statement = conn.createStatement()) {
                statement.executeUpdate(usersSql);
                statement.executeUpdate(authsSql);
                statement.executeUpdate(gamesSql);
            }
        } catch (SQLException ex) {
            throw new DataAccessException("failed to create tables", ex);
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
                return new User(result.getString("username"), result.getString("password"), result.getString("email"));
            } catch (SQLException ex) {
                throw new DataAccessException("failed to get user", ex);
            }
        }
    }

}
