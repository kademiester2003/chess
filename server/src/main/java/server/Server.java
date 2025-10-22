package server;

import com.google.gson.Gson;
import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import dataaccess.MemoryDataAccess;
import model.User;
import io.javalin.Javalin;
import io.javalin.http.Context;
import service.UserService;

public class Server {

    private final DataAccess dao;
    private final Javalin server;
    private final UserService userService;
    private final Gson gson =  new Gson();

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
        server.post("/user", ctx -> {
            try {
                UserService.RegisterRequest req = gson.fromJson(ctx.body(), UserService.RegisterRequest.class);
                var res = userService.register(req);
                ctx.status(200).json(res);
            } catch (IllegalArgumentException ex) {
                ctx.status(400).json(error("Error: bad request"));
            } catch (DataAccessException ex) {
                if ("already taken".equals(ex.getMessage())) ctx.status(403).json(error("Error: already taken"));
                else ctx.status(500).json(error("Error: " + ex.getMessage()));
            } catch (Exception ex) {
                ctx.status(500).json(error("Error: " + ex.getMessage()));
            }
        });
        //login
        server.post("/session", ctx -> {
            try {
                UserService.LoginRequest req = gson.fromJson(ctx.body(), UserService.LoginRequest.class);
                var res = userService.login(req);
                ctx.status(200).json(res);
            } catch (IllegalArgumentException ex) {
                ctx.status(400).json(error("Error: bad request"));
            } catch (DataAccessException ex) {
                ctx.status(401).json(error("Error: unauthorized"));
            } catch (Exception ex) {
                ctx.status(500).json(error("Error: " + ex.getMessage()));
            }
        });
        //logout
        server.delete("/session", ctx -> {
            String token = ctx.header("authorization");
            try {
                userService.logout(token);
                ctx.status(200).result("{}");
            } catch (IllegalArgumentException | DataAccessException ex) {
                ctx.status(401).json(error("Error: unauthorized"));
            } catch (Exception ex) {
                ctx.status(500).json(error("Error: " + ex.getMessage()));
            }
        });
        //listGames
        server.get("/game", ctx -> {});
        //createGame
        server.post("/game", ctx -> {});
        //joinGame
        server.put("/game", ctx -> {});
    }

    private void registerExceptionHandlers() {
        server.exception(Exception.class, (e, ctx) -> {
            ctx.status(500).json("Error: " + error(e.getMessage()));
        });
    }

    private Object error(String msg) {
        return new Object() { @SuppressWarnings("unused") public final String message = msg;};
    }

    public int run(int desiredPort) {
        server.start(desiredPort);
        return server.port();
    }

    public void stop() {
        server.stop();
    }
}
