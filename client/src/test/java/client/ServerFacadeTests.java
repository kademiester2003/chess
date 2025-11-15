package client;

import org.junit.jupiter.api.*;
import server.Server;

import java.net.HttpURLConnection;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.*;

public class ServerFacadeTests {

    private static Server server;
    private static ServerFacade facade;
    private static int port;
    @BeforeAll
    public static void init() {
        server = new Server();
        port = server.run(0);

        System.out.println("Started test HTTP server on port " + port);

        facade = new ServerFacade("localhost", String.valueOf(port));
    }

    @AfterAll
    public static void shutdown() {
        server.stop();
    }

    @BeforeEach
    public void clearDB() throws Exception {
        HttpURLConnection connection =
                (HttpURLConnection) new URL("http://localhost:" + port + "/db").openConnection();

        connection.setRequestMethod("DELETE");
        connection.connect();

        assertEquals(200, connection.getResponseCode());
    }

    @Test
    public void registerPositive() throws Exception {
        var req = new ServerFacade.RegisterRequest();
        req.username = "alice";
        req.password = "pass";
        req.email = "a@mail.com";

        var res = facade.register(req);
        assertNotNull(res.authToken);
        assertEquals("alice", res.username);
    }

    @Test
    public void registerNegativeDuplicateUser() throws Exception {
        var req = new ServerFacade.RegisterRequest();
        req.username = "bob";
        req.password = "pass";
        req.email = "b@mail.com";

        facade.register(req);
        var res2 = facade.register(req);

        assertNull(res2.authToken);
        assertNotNull(res2.message);
    }

    @Test
    public void loginPositive() throws Exception {
        var req = new ServerFacade.RegisterRequest();
        req.username = "mike";
        req.password = "secret";
        req.email = "m@mail.com";

        facade.register(req);

        var login = new ServerFacade.LoginRequest();
        login.username = "mike";
        login.password = "secret";

        var res = facade.login(login);

        assertNotNull(res.authToken);
        assertEquals("mike", res.username);
    }

    @Test
    public void loginNegativeWrongPassword() throws Exception {
        var req = new ServerFacade.RegisterRequest();
        req.username = "sam";
        req.password = "goodpw";
        req.email = "s@mail.com";

        facade.register(req);

        var login = new ServerFacade.LoginRequest();
        login.username = "sam";
        login.password = "badpw";

        var res = facade.login(login);

        assertNull(res.authToken);
        assertNotNull(res.message);
    }

    @Test
    public void logoutPositive() throws Exception {
        var req = new ServerFacade.RegisterRequest();
        req.username = "jen";
        req.password = "pw";
        req.email = "j@mail.com";

        var reg = facade.register(req);

        var res = facade.logout(reg.authToken);

        assertNull(res.message);
    }

    @Test
    public void logoutNegativeInvalidToken() throws Exception {
        var res = facade.logout("bad-token");

        assertNotNull(res.message);
    }

    @Test
    public void createGamePositive() throws Exception {
        var reg = facade.register(newReq("kate"));
        var req = new ServerFacade.CreateGameRequest("CoolGame");
        var res = facade.createGame(req, reg.authToken);

        assertNotNull(res.gameID);
    }

    @Test
    public void createGameNegativeInvalidToken() throws Exception {
        var req = new ServerFacade.CreateGameRequest("FailGame");
        var res = facade.createGame(req, "bad-token");

        assertNotNull(res.message);
    }

    @Test
    public void listGamesPositive() throws Exception {
        var reg = facade.register(newReq("tom"));

        var create1 = facade.createGame(new ServerFacade.CreateGameRequest("One"), reg.authToken);
        var create2 = facade.createGame(new ServerFacade.CreateGameRequest("Two"), reg.authToken);

        var res = facade.listGames(reg.authToken);

        assertNotNull(res.games);
        assertTrue(res.games.length >= 2, "Expected at least two games");

        boolean foundOne = false;
        boolean foundTwo = false;
        for (var g : res.games) {
            if (g.gameName != null && g.gameName.equals("One")) {foundOne = true;}
            if (g.gameName != null && g.gameName.equals("Two")) {foundTwo = true;}
        }

        assertTrue(foundOne, "Game named 'One' was not found in listGames result");
        assertTrue(foundTwo, "Game named 'Two' was not found in listGames result");
    }

    @Test
    public void listGamesNegativeInvalidToken() throws Exception {
        var res = facade.listGames("bad-token");
        assertNotNull(res.message);
    }

    @Test
    public void joinGamePositive() throws Exception {
        var creator = facade.register(newReq("creator"));
        var create = facade.createGame(new ServerFacade.CreateGameRequest("JoinMe"), creator.authToken);

        var joiner = facade.register(newReq("joiner"));
        var req = new ServerFacade.JoinGameRequest("BLACK", create.gameID);
        var res = facade.joinGame(req, joiner.authToken);

        assertNull(res.message);
    }

    @Test
    public void joinGameNegativeInvalidGame() throws Exception {
        var reg = facade.register(newReq("rob"));

        var req = new ServerFacade.JoinGameRequest("BLACK", 999999);
        var res = facade.joinGame(req, reg.authToken);

        assertNotNull(res.message);
    }

    @Test
    public void joinGameNegativeInvalidToken() throws Exception {
        var creator = facade.register(newReq("alice2"));
        var create = facade.createGame(new ServerFacade.CreateGameRequest("NoAuthGame"), creator.authToken);

        var req = new ServerFacade.JoinGameRequest("WHITE", create.gameID);
        var res = facade.joinGame(req, "bad-token");

        assertNotNull(res.message);
    }

    private ServerFacade.RegisterRequest newReq(String user) {
        var req = new ServerFacade.RegisterRequest();
        req.username = user;
        req.password = "pw";
        req.email = user + "@mail.com";
        return req;
    }
}
