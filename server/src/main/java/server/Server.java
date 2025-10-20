package server;

import com.google.gson.Gson;
import dataaccess.DataAccess;
import dataaccess.MemoryDataAccess;
import datamodel.User;
import io.javalin.*;
import io.javalin.http.Context;
import service.UserService;

import java.util.Collection;


public class Server {

    private final Javalin server;
    private final UserService userService;

    public Server() {
        DataAccess dataAccess = new MemoryDataAccess();
        userService = new UserService(dataAccess);
        server = Javalin.create(config -> config.staticFiles.add("web"));

        server.delete("db", ctx -> ctx.result( "{}"));
        server.post("user", this::register);
        server.post("session", this::login);

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

    private void listGames(Context ctx) {}

    private void createGame(Context ctx) {}

    private void joinGame(Context ctx) {}

    private void clear(Context ctx) {}

    public int run(int desiredPort) {
        server.start(desiredPort);
        return server.port();
    }

    public void stop() {
        server.stop();
    }
}
