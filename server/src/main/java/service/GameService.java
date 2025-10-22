package service;

import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import model.*;

import java.util.List;

public class GameService {
    private final DataAccess dao;

    public GameService(DataAccess dao) {
        this.dao = dao;
    }
}
