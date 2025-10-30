package dataaccess;

import com.google.gson.Gson;

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

}
