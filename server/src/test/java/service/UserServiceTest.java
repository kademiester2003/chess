package service;

import dataaccess.MemoryDataAccess;
import model.Auth;
import model.User;
import org.junit.jupiter.api.*;
import dataaccess.DataAccessException;

import static org.junit.jupiter.api.Assertions.*;

public class UserServiceTest {
    private MemoryDataAccess dao;
    private UserService userService;

    @BeforeEach
    public void setup() {
        dao = new MemoryDataAccess();
        userService = new UserService(dao);
    }

    @Test
    public void registerSuccess() throws Exception {
        var req = new UserService.RegisterRequest("alice", "pw", "a@x.com");
        var res = userService.register(req);
        assertEquals("alice", res.username());
        assertNotNull(res.authToken());

        Auth auth = dao.getAuth(res.authToken());
        assertNotNull(auth);
        assertEquals("alice", auth.username());
        User user = dao.getUser("alice");
        assertNotNull(user);
        assertEquals("a@x.com", user.email());
    }

    @Test
    public void registerAlreadyTaken() throws Exception {
        dao.createUser(new User("bob","p","b@b"));
        var req = new UserService.RegisterRequest("bob","p2","b2@b");
        DataAccessException ex = assertThrows(DataAccessException.class, () -> userService.register(req));
        assertEquals("already taken", ex.getMessage());
    }

    @Test
    public void loginSuccessAndLogout() throws Exception {
        dao.createUser(new User("carol","secret","c@c"));
        var login = new UserService.LoginRequest("carol","secret");
        var res = userService.login(login);
        assertEquals("carol", res.username());
        assertNotNull(res.authToken());

        userService.logout(res.authToken());
        assertNull(dao.getAuth(res.authToken()));
    }

    @Test
    public void loginUnauthorized() {
        DataAccessException ex = assertThrows(DataAccessException.class, () -> userService.login(new UserService.LoginRequest("no","no")));
        assertEquals("unauthorized", ex.getMessage());
    }
}
