package server;

import com.google.gson.Gson;
import dataaccess.DataAccess;
import dataaccess.MemoryDataAccess;
import model.User;
import io.javalin.*;
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

    private void registerEndpoints() {}

    private void registerExceptionHandlers() {}

    private void register(Context ctx) {
        var serializer = new Gson();
        var req = serializer.fromJson(ctx.body(), User.class);
        var res = userService.register(req);
        ctx.result(serializer.toJson(res));
    }

    private void login(Context ctx) {
        var serializer = new Gson();
        var req = serializer.fromJson(ctx.body(), User.class);
        var res = userService.login(req);
        ctx.result(serializer.toJson(res));
    }

    private void logout(Context ctx) {}

    private void listGames(Context ctx) {
        var serializer = new Gson();
        var req = serializer.fromJson(ctx.body(), User.class);
        var res = userService.listGames(req);
        ctx.result(serializer.toJson(res));
    }

    private void createGame(Context ctx) {
        var serializer = new Gson();
        var req = serializer.fromJson(ctx.body(), User.class);
        var res = userService.createGame(req);
        ctx.result(serializer.toJson(res));
    }

    private void joinGame(Context ctx) {
        var serializer = new Gson();
        var req = serializer.fromJson(ctx.body(), User.class);
        var res = userService.joinGame(req);
        ctx.result(serializer.toJson(res));
    }

    private void clear(Context ctx) {}

    public int run(int desiredPort) {
        server.start(desiredPort);
        return server.port();
    }

    public void stop() {
        server.stop();
    }
}
