package server;

import com.google.gson.Gson;
import dataaccess.DataAccess;
import dataaccess.MemoryDataAccess;
import model.User;
import io.javalin.Javalin;
import io.javalin.http.Context;
import service.UserService;

public class Server {

    private final DataAccess dao;
    private final Javalin server;
    private final UserService userService;

    public Server() {
        this.dao =  new MemoryDataAccess();
        this.userService = new UserService(dao);

        server = Javalin.create(config -> config.staticFiles.add("web"));
        registerEndpoints();
        registerExceptionHandlers();
    }

    private void registerEndpoints() {
        //clear
        server.delete("/db", ctx -> {});
        //register
        server.post("/user", ctx -> {});
        //login
        server.post("/session", ctx -> {});
        //logout
        server.delete("/session", ctx -> {});
        //listGames
        server.get("/game", ctx -> {});
        //createGame
        server.post("/game", ctx -> {});
        //joinGame
        server.put("/game", ctx -> {});
    }

    private void registerExceptionHandlers() {}

    private Object error(String msg) {
        return new Object() {public final String message = msg;};
    }

    public int run(int desiredPort) {
        server.start(desiredPort);
        return server.port();
    }

    public void stop() {
        server.stop();
    }
}
