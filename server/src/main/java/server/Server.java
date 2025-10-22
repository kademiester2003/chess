package server;

import com.google.gson.Gson;
import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import dataaccess.MemoryDataAccess;
import io.javalin.Javalin;
import service.UserService;
import service.GameService;

public class Server {

    private final DataAccess dao;
    private final Javalin server;
    private final UserService userService;
    private final GameService gameService;
    private final Gson gson = new Gson();

    public Server() {
        this.dao =  new MemoryDataAccess();
        this.userService = new UserService(dao);
        this.gameService = new GameService(dao);

        server = Javalin.create(config -> config.staticFiles.add("web"));
        registerEndpoints();
        registerExceptionHandlers();
    }

    private void registerEndpoints() {
        //clear
        server.delete("/db", ctx -> {
            try {
                dao.clear();
                ctx.status(200).result("{}");
            } catch (Exception e) {
                ctx.status(500).json(error("Error: " + e.getMessage()));
            }
        });
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
        server.get("/game", ctx -> {
            String token =  ctx.header("authorization");
            try {
                var res = gameService.listGames(token);
                ctx.status(200).result(gson.toJson(res));
            } catch (IllegalArgumentException ex) {
                ctx.status(401).json(error("Error: unauthorized"));
            } catch (DataAccessException ex) {
                if ("unauthorized".equals(ex.getMessage())) ctx.status(401).json(error("Error: unauthorized"));
                else ctx.status(500).json(error("Error: " + ex.getMessage()));
            } catch (Exception ex) {
                ctx.status(500).json(error("Error: " + ex.getMessage()));
            }
        });
        //createGame
        server.post("/game", ctx -> {
            String token = ctx.header("authorization");
            try {
                var req = gson.fromJson(ctx.body(), GameService.CreateGamesRequest.class);
                var res = gameService.createGame(token, req);
                ctx.status(200).result(gson.toJson(res));
            } catch (IllegalArgumentException ex) {
                ctx.status(400).json(error("Error: bad request"));
            } catch (DataAccessException ex) {
                if ("unauthorized".equals(ex.getMessage())) ctx.status(401).json(error("Error: unauthorized"));
                else ctx.status(500).json(error("Error: " + ex.getMessage()));
            } catch (Exception ex) {
                ctx.status(500).json(error("Error: " + ex.getMessage()));
            }
        });
        //joinGame
        server.put("/game", ctx -> {
            String token = ctx.header("authorization");
            try {
                var req = gson.fromJson(ctx.body(), GameService.JoinGameRequest.class);
                gameService.joinGame(token, req);
                ctx.status(200).result("{}");
            } catch (IllegalArgumentException ex) {
                ctx.status(400).json(error("Error: bad request"));
            } catch (DataAccessException ex) {
                if ("unauthorized".equals(ex.getMessage())) ctx.status(401).json(error("Error: unauthorized"));
                else if ("already taken".equals(ex.getMessage())) ctx.status(403).json(error("Error: already taken"));
                else ctx.status(500).json(error("Error: " + ex.getMessage()));
            } catch (Exception ex) {
                ctx.status(500).json(error("Error: " + ex.getMessage()));
            }
        });
    }

    private void registerExceptionHandlers() {
        server.exception(Exception.class, (e, ctx) -> ctx.status(500).json("Error: " + error(e.getMessage())));
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
