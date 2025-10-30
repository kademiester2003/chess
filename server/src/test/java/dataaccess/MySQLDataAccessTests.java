package dataaccess;

import chess.ChessGame;
import model.Auth;
import model.Game;
import model.User;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MySQLDataAccess.
 * Positive and negative tests are included for each public method,
 * except clear(), which only has a positive test.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MySQLDataAccessTests {

    private static MySQLDataAccess dao;

    private static final User TEST_USER = new User("ExistingUser", "password123", "email@example.com");

    @BeforeAll
    static void setupDatabase() throws DataAccessException {
        dao = new MySQLDataAccess();
        dao.clear();
    }

    @BeforeEach
    void clearDatabaseBeforeEach() throws DataAccessException {
        dao.clear();
    }

    // region clear()

    @Test
    @Order(1)
    @DisplayName("Clear Database - Positive")
    void clearDatabasePositive() throws DataAccessException {
        dao.createUser(TEST_USER);
        assertNotNull(dao.getUser(TEST_USER.username()));
        dao.clear();
        assertNull(dao.getUser(TEST_USER.username()), "Database should be empty after clear()");
    }

    // endregion

    // region createUser()

    @Test
    @Order(2)
    @DisplayName("Create User - Positive")
    void createUserPositive() throws DataAccessException {
        dao.createUser(TEST_USER);
        User found = dao.getUser(TEST_USER.username());
        assertNotNull(found);
        assertEquals(TEST_USER.username(), found.username());
    }

    @Test
    @Order(3)
    @DisplayName("Create User - Negative (duplicate username)")
    void createUserNegative() throws DataAccessException {
        dao.createUser(TEST_USER);
        assertThrows(DataAccessException.class, () -> dao.createUser(TEST_USER),
                "Creating a user with an existing username should throw");
    }

    // endregion

    // region getUser()

    @Test
    @Order(4)
    @DisplayName("Get User - Positive")
    void getUserPositive() throws DataAccessException {
        dao.createUser(TEST_USER);
        User found = dao.getUser(TEST_USER.username());
        assertNotNull(found);
        assertEquals(TEST_USER.email(), found.email());
    }

    @Test
    @Order(5)
    @DisplayName("Get User - Negative (nonexistent)")
    void getUserNegative() throws DataAccessException {
        User found = dao.getUser("fakeUser");
        assertNull(found, "Nonexistent user should return null");
    }

    // endregion

    // region createAuth()

    @Test
    @Order(6)
    @DisplayName("Create Auth - Positive")
    void createAuthPositive() throws DataAccessException {
        dao.createUser(TEST_USER);
        Auth auth = new Auth("token123", TEST_USER.username());
        dao.createAuth(auth);

        Auth found = dao.getAuth(auth.authToken());
        assertNotNull(found);
        assertEquals(TEST_USER.username(), found.username());
    }

    @Test
    @Order(7)
    @DisplayName("Create Auth - Negative (duplicate token)")
    void createAuthNegative() throws DataAccessException {
        dao.createUser(TEST_USER);
        Auth auth = new Auth("dupToken", TEST_USER.username());
        dao.createAuth(auth);
        assertThrows(DataAccessException.class, () -> dao.createAuth(auth),
                "Duplicate auth tokens should cause an error");
    }

    // endregion

    // region getAuth()

    @Test
    @Order(8)
    @DisplayName("Get Auth - Positive")
    void getAuthPositive() throws DataAccessException {
        dao.createUser(TEST_USER);
        Auth auth = new Auth("findToken", TEST_USER.username());
        dao.createAuth(auth);

        Auth found = dao.getAuth("findToken");
        assertNotNull(found);
        assertEquals(TEST_USER.username(), found.username());
    }

    @Test
    @Order(9)
    @DisplayName("Get Auth - Negative (nonexistent)")
    void getAuthNegative() throws DataAccessException {
        Auth found = dao.getAuth("missingToken");
        assertNull(found);
    }

    // endregion

    // region deleteAuth()

    @Test
    @Order(10)
    @DisplayName("Delete Auth - Positive")
    void deleteAuthPositive() throws DataAccessException {
        dao.createUser(TEST_USER);
        Auth auth = new Auth("deleteToken", TEST_USER.username());
        dao.createAuth(auth);

        dao.deleteAuth("deleteToken");
        assertNull(dao.getAuth("deleteToken"));
    }

    @Test
    @Order(11)
    @DisplayName("Delete Auth - Negative (nonexistent token)")
    void deleteAuthNegative() {
        // Should not throw, but simply not delete anything
        assertDoesNotThrow(() -> dao.deleteAuth("ghostToken"));
    }

    // endregion

    // region createGame()

    @Test
    @Order(12)
    @DisplayName("Create Game - Positive")
    void createGamePositive() throws DataAccessException {
        Game game = new Game(0, null, null, "Test Game", new ChessGame());
        int gameID = dao.createGame(game);

        Game found = dao.getGame(gameID);
        assertNotNull(found);
        assertEquals("Test Game", found.gameName());
    }

    @Test
    @Order(13)
    @DisplayName("Create Game - Negative (null game)")
    void createGameNegative() {
        assertThrows(DataAccessException.class, () -> dao.createGame(null),
                "Null game should throw DataAccessException");
    }

    // endregion

    // region getGame()

    @Test
    @Order(14)
    @DisplayName("Get Game - Positive")
    void getGamePositive() throws DataAccessException {
        // Arrange: Create users first (foreign key requirement)
        dao.createUser(new User("whiteUser", "pass", "white@example.com"));
        dao.createUser(new User("blackUser", "pass", "black@example.com"));

        // Create a game with those users
        ChessGame chessGame = new ChessGame();
        Game game = new Game(0, "whiteUser", "blackUser", "Test Game", chessGame);
        int gameId = dao.createGame(game);

        // Act
        Game retrieved = dao.getGame(gameId);

        // Assert
        assertNotNull(retrieved);
        assertEquals("whiteUser", retrieved.whiteUsername());
        assertEquals("blackUser", retrieved.blackUsername());
        assertEquals("Test Game", retrieved.gameName());
        assertNotNull(retrieved.game());
    }

    @Test
    @Order(15)
    @DisplayName("Get Game - Negative (nonexistent)")
    void getGameNegative() throws DataAccessException {
        Game found = dao.getGame(99999);
        assertNull(found);
    }

    // endregion

    // region listGames()

    @Test
    @Order(16)
    @DisplayName("List Games - Positive")
    void listGamesPositive() throws DataAccessException {
        dao.createGame(new Game(0, null, null, "A", null));
        dao.createGame(new Game(0, null, null, "B", null));
        List<Game> games = dao.listGames();
        assertTrue(games.size() >= 2);
    }

    @Test
    @Order(17)
    @DisplayName("List Games - Negative (empty database)")
    void listGamesNegative() throws DataAccessException {
        dao.clear();
        List<Game> games = dao.listGames();
        assertTrue(games.isEmpty());
    }

    // endregion

    // region updateGame()

    @Test
    @Order(18)
    @DisplayName("Update Game - Positive")
    void updateGamePositive() throws DataAccessException {
        // Arrange: Create required users
        dao.createUser(new User("whiteUser", "pass", "white@example.com"));
        dao.createUser(new User("blackUser", "pass", "black@example.com"));

        // Create initial game
        ChessGame chessGame = new ChessGame();
        Game game = new Game(0, "whiteUser", "blackUser", "Original Game", chessGame);
        int gameId = dao.createGame(game);

        // Act: Update the game name
        Game updated = new Game(gameId, "whiteUser", "blackUser", "Updated Game", chessGame);
        dao.updateGame(updated);

        // Assert: Verify update
        Game result = dao.getGame(gameId);
        assertNotNull(result);
        assertEquals("Updated Game", result.gameName());
    }
    @Test
    @Order(19)
    @DisplayName("Update Game - Negative (nonexistent ID)")
    void updateGameNegative() {
        Game fake = new Game(9999, "w", "b", "Nonexistent", new ChessGame());
        assertThrows(DataAccessException.class, () -> dao.updateGame(fake));
    }

    // endregion
}