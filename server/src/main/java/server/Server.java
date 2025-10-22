package server;

import com.google.gson.Gson;
import dataaccess.DataAccess;
import dataaccess.MemoryDataAccess;
import model.User;
import io.javalin.*;
import io.javalin.http.Context;
import service.UserService;


public class Server {

    private final Javalin server;
    private final UserService userService;

    public Server() {
        DataAccess dataAccess = new MemoryDataAccess();
        userService = new UserService(dataAccess);
        server = Javalin.create(config -> config.staticFiles.add("web"));

        //clear (does not call clear)
        server.delete("db", ctx -> ctx.result( "{}"));

        //register
        server.post("user", this::register);

        //login
        server.post("session", this::login);

        //logout (does not call logout)
        server.delete("session", ctx -> ctx.result("{}"));

        //listGames
        server.get("game", this::listGames);

        //createGame
        server.post("game", this::createGame);

        //joinGame
        server.put("game", this::joinGame);

        // Register your endpoints and exception handlers here.

    }

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
