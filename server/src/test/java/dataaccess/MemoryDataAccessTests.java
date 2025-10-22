package dataaccess;

import model.User;
import model.Game;
import model.Auth;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the in-memory data access implementation.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MemoryDataAccessTests {

    private static MemoryDataAccess dao;

    @BeforeAll
    static void setup() {
        dao = new MemoryDataAccess();
    }

    @BeforeEach
    void clearBeforeEach() {
        dao.clear();
    }

    @Test
    @Order(1)
    @DisplayName("Insert and Retrieve User Successfully")
    void insertAndRetrieveUserSuccess() throws DataAccessException {
        User user = new User("alice", "password123", "alice@mail.com");
        dao.createUser(user);

        User retrieved = dao.getUser("alice");
        assertNotNull(retrieved, "User should be retrievable after insertion");
        assertEquals("alice", retrieved.username());
        assertEquals("password123", retrieved.password());
        assertEquals("alice@mail.com", retrieved.email());
    }

    @Test
    @Order(2)
    @DisplayName("Duplicate User Insertion Throws Exception")
    void insertDuplicateUserThrowsException() throws DataAccessException {
        User user = new User("bob", "123", "bob@mail.com");
        dao.createUser(user);
        assertThrows(DataAccessException.class, () -> dao.createUser(user),
                "Inserting a duplicate username should throw DataAccessException");
    }

    @Test
    @Order(3)
    @DisplayName("Get Nonexistent User Returns Null")
    void getNonexistentUserReturnsNull() {
        assertNull(dao.getUser("notAUser"), "Should return null for unknown username");
    }

    @Test
    @Order(4)
    @DisplayName("Create and Retrieve Auth Token")
    void createAndRetrieveAuthToken() throws DataAccessException {
        Auth auth = new Auth("token123", "alice");
        dao.createAuth(auth);

        Auth retrieved = dao.getAuth("token123");
        assertNotNull(retrieved, "Auth should be retrievable after creation");
        assertEquals("alice", retrieved.username());
    }

    @Test
    @Order(5)
    @DisplayName("Delete Auth Token")
    void deleteAuthTokenSuccess() throws DataAccessException {
        Auth auth = new Auth("tokenToDelete", "bob");
        dao.createAuth(auth);

        dao.deleteAuth("tokenToDelete");
        assertNull(dao.getAuth("tokenToDelete"), "Auth should be removed after deletion");
    }

    @Test
    @Order(6)
    @DisplayName("Create and Retrieve Game")
    void createAndRetrieveGameSuccess() {
        Game game = new Game(0, null, null, "TestGame", null);
        int gameId = dao.createGame(game);

        assertTrue(gameId > 0, "Game ID should be positive");

        Game retrieved = dao.getGame(gameId);
        assertNotNull(retrieved, "Game should be retrievable after creation");
        assertEquals(gameId, retrieved.gameID());
        assertEquals("TestGame", retrieved.gameName());
    }
    @Test
    @Order(7)
    @DisplayName("List Games Returns All Created Games")
    void listGamesSuccess() {
        dao.createGame(new Game(0, null, null, "Game1", null));
        dao.createGame(new Game(0, null, null, "Game2", null));
        dao.createGame(new Game(0, null, null, "Game3", null));

        var games = dao.listGames();
        assertEquals(3, games.size(), "Should return all created games");
    }

    @Test
    @Order(8)
    @DisplayName("Clear Removes All Data")
    void clearRemovesAllData() throws DataAccessException {
        dao.createUser(new User("a", "p", "e@mail.com"));
        dao.createAuth(new Auth("t1", "a"));
        dao.createGame(new Game(0, null, null, "Clear Game", null));

        dao.clear();

        assertNull(dao.getUser("a"), "All users should be cleared");
        assertNull(dao.getAuth("t1"), "All auth tokens should be cleared");
        assertTrue(dao.listGames().isEmpty(), "All games should be cleared");
    }
}