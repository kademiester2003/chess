package service;

import dataaccess.DataAccess;
import datamodel.RegistrationResult;
import datamodel.LoginResult;
import datamodel.User;
import java.util.UUID;

public class UserService {
    private final DataAccess dataAccess;
    public UserService(DataAccess dataAccess) {
        this.dataAccess = dataAccess;
    }

    public RegistrationResult register(User user) {
        dataAccess.saveUser(user);
        String authToken = UUID.randomUUID().toString();
        return new RegistrationResult(user.username(), authToken);
    }

    public LoginResult login(User user) {
        dataAccess.getUser(user.username());
        String authToken = UUID.randomUUID().toString();
        return new LoginResult(user.username(), authToken);
    }
}
