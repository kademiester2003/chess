package server;

import com.google.gson.Gson;
import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import dataaccess.MemoryDataAccess;
import dataaccess.MySQLDataAccess;
import io.javalin.Javalin;
import io.javalin.http.Context;
import service.UserService;
import service.GameService;
import server.websocket.GameWebSocketEndpoint;

public class Server {

    private final DataAccess dao;
    private final Javalin server;
    private final UserService userService;
    private final GameService gameService;
    private final Gson gson = new Gson();
    private GameWebSocketEndpoint handler;

    public Server() {
        DataAccess tempDao;
        try {
            tempDao = new MySQLDataAccess();
        } catch (DataAccessException ex) {
            ex.printStackTrace();
            tempDao = new MemoryDataAccess();
        }
        this.dao = tempDao;
        this.userService = new UserService(dao);
        this.gameService = new GameService(dao);

        this.handler = new GameWebSocketEndpoint(dao);

        server = Javalin.create(config -> config.staticFiles.add("web"));
        registerEndpoints();
    }

    private void handleRequest(Context ctx, RunnableWithException action) {
        try {
            action.run();
        } catch (IllegalArgumentException ex) {
            if (ex.getMessage().equals("bad request")) {
                respondWithError(ctx, 400, "Error: bad request");
            }
            else {
                respondWithError(ctx, 401, "Error: unauthorized");
            }
        } catch (DataAccessException ex) {
            handleDataAccessException(ctx, ex);
        } catch (Exception ex) {
            respondWithError(ctx, 500, ex.getMessage());
        }
    }

    private void respondWithError(Context ctx, int status, String message) {
        if (message == null || message.isBlank()) {
            message = "Error: Unknown server error.";
        } else if (!message.toLowerCase().contains("error")) {
            message = "Error: " + message;
        }
        ctx.status(status).result(String.format("{\"message\": \"%s\"}", message));
    }

    private void handleDataAccessException(io.javalin.http.Context ctx, DataAccessException ex) {
        switch (ex.getMessage()) {
            case "unauthorized" -> respondWithError(ctx, 401, "Error: unauthorized");
            case "already taken" -> respondWithError(ctx, 403, "Error: already taken");
            case "game not found" -> respondWithError(ctx, 400, "Error: game not found");
            default -> respondWithError(ctx, 500, ex.getMessage());
        }
    }

    @FunctionalInterface
    private interface RunnableWithException {
        void run() throws Exception;
    }

    private void registerEndpoints() {
        // clear
        server.delete("/db", ctx -> {
            try {
                dao.clear();
                ctx.status(200).result("{}");
            } catch (Exception ex) {
                respondWithError(ctx, 500, ex.getMessage());
            }
        });

        // register
        server.post("/user", ctx -> handleRequest(ctx, () -> {
            var req = gson.fromJson(ctx.body(), UserService.RegisterRequest.class);
            var res = userService.register(req);
            ctx.status(200).json(gson.toJson(res));
        }));

        // login
        server.post("/session", ctx -> handleRequest(ctx, () -> {
            var req = gson.fromJson(ctx.body(), UserService.LoginRequest.class);
            var res = userService.login(req);
            ctx.status(200).json(gson.toJson(res));
        }));

        // logout
        server.delete("/session", ctx -> handleRequest(ctx, () -> {
            String token = ctx.header("authorization");
            userService.logout(token);
            ctx.status(200).result("{}");
        }));

        // listGames
        server.get("/game", ctx -> handleRequest(ctx, () -> {
            String token = ctx.header("authorization");
            var res = gameService.listGames(token);
            ctx.status(200).json(gson.toJson(res));
        }));

        // createGame
        server.post("/game", ctx -> handleRequest(ctx, () -> {
            String token = ctx.header("authorization");
            var req = gson.fromJson(ctx.body(), GameService.CreateGameRequest.class);
            var res = gameService.createGame(token, req);
            ctx.status(200).json(gson.toJson(res));
        }));

        // joinGame
        server.put("/game", ctx -> handleRequest(ctx, () -> {
            String token = ctx.header("authorization");
            var req = gson.fromJson(ctx.body(), GameService.JoinGameRequest.class);
            gameService.joinGame(token, req);
            ctx.status(200).result("{}");
        }));

        //ws
        server.ws("/ws", ws -> {
            ws.onConnect(handler::onConnect);
            ws.onMessage(ctx -> handler.onMessage(ctx, ctx.message()));
            ws.onClose(handler::onClose);
            ws.onError(ctx -> handler.onError(ctx, ctx.error()));
        });
    }

    public int run(int desiredPort) {
        server.start(desiredPort);
        return server.port();
    }

    public void stop() {
        server.stop();
    }
}
