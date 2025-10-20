package dataaccess;

import datamodel.User;

import java.util.HashMap;

public class MemoryDataAccess implements DataAccess {
    private HashMap<String, User> users = new HashMap<>();
    @Override
    public void saveUser(User user) {
        users.put(user.username(), user);
    }

    @Override
    public void createUser(User user) {

    }

    @Override
    public void getUser(String username) {
        users.get(username);
    }
}
