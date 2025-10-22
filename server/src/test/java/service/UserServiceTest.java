package service;

import dataaccess.MemoryDataAccess;
import model.User;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

public class UserServiceTest {

    @Test
    void clear() {

    }

    @Test
    void register() throws Exception {
        var user = new User("joe", "j@j", "j");
        var at = "xyz";

        var dataAccess = new MemoryDataAccess();
        var service =  new UserService(dataAccess);
        var res = service.register(user);
        assertNotNull(res);
        assertEquals(res.username(), user.username());
        assertNotNull(res.authToken());
        assertEquals((String.class), res.authToken().getClass());
    }
}
