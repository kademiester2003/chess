package client;

import org.junit.jupiter.api.*;
import server.Server;

import java.net.HttpURLConnection;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.*;

public class ServerFacadeTests {

    private static Server server;
    private static ServerFacade facade;
    private static int port;   // <-- FIX: Make port available to all tests

    @BeforeAll
    public static void init() {
        server = new Server();
        port = server.run(0);      // <-- store port in static field

        System.out.println("Started test HTTP server on port " + port);

        facade = new ServerFacade("localhost", String.valueOf(port));
    }

    @AfterAll
    public static void shutdown() {
        server.stop();
    }

    @BeforeEach
    public void clearDB() throws Exception {
        // FIX: use static port field
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
    public void registerNegative_duplicateUser() throws Exception {
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
    public void loginNegative_wrongPassword() throws Exception {
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

        assertNull(res.message);   // 200 OK produces "{}"
    }

    @Test
    public void logoutNegative_invalidToken() throws Exception {
        var res = facade.logout("bad-token");

        assertNotNull(res.message);
    }
}
