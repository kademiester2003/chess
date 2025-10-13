package service;

import datamodel.RegistrationResult;
import datamodel.User;

public class UserService {
    public RegistrationResult register(User user) {
        return new RegistrationResult(user.username(), "zyyz");
    }
}
