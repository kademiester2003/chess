package dataaccess;

import datamodel.User;

public interface DataAccess {
    void saveUser(User user);
    void createUser(User user);
    void getUser(String username);
}
