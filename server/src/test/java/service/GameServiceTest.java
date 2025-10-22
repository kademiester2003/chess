package service;

import dataaccess.MemoryDataAccess;
import model.Auth;
import model.User;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

public class GameServiceTest {
    private MemoryDataAccess dao;
    private GameService gameService;
    private UserService userService;

    @BeforeEach
    public void setup() throws Exception {
        dao = new MemoryDataAccess();
        gameService = new GameService(dao);
        userService = new UserService(dao);
        // create user and auth token
        dao.createUser(new User("p1","pw","e@e"));
        var token = UserService.generateToken();
        dao.createAuth(new Auth(token, "p1"));
        // store token for tests via dao lookup
    }

    @Test
    public void createListJoinGameFlow() throws Exception {
        // create user and token
        var reg = userService.register(new UserService.RegisterRequest("u1","pw","u1@u"));
        String token = reg.authToken();

        var createReq = new GameService.CreateGameRequest("MyGame");
        var createRes = gameService.createGame(token, createReq);
        assertTrue(createRes.gameID() > 0);

        var list = gameService.listGames(token);
        assertNotNull(list.games());
        assertTrue(list.games().stream().anyMatch(g -> g.gameName().equals("MyGame")));

        // join as WHITE
        var joinReq = new GameService.JoinGameRequest("WHITE", createRes.gameID());
        gameService.joinGame(token, joinReq);

        var game = dao.getGame(createRes.gameID());
        assertEquals("u1", game.whiteUsername());
    }

    @Test
    public void joinAlreadyTaken() throws Exception {
        var reg1 = userService.register(new UserService.RegisterRequest("j1","pw","j1@j"));
        var reg2 = userService.register(new UserService.RegisterRequest("j2","pw","j2@j"));
        int id = gameService.createGame(reg1.authToken(), new GameService.CreateGameRequest("G")).gameID();

        gameService.joinGame(reg1.authToken(), new GameService.JoinGameRequest("WHITE", id));
        assertThrows(Exception.class, () -> gameService.joinGame(reg2.authToken(), new GameService.JoinGameRequest("WHITE", id)));
    }
}