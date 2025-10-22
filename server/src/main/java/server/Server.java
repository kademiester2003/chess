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
    }

    private void registerEndpoints() {
        //clear
        server.delete("/db", ctx -> {
            try {
                dao.clear();
                ctx.status(200).result("{}");
            } catch (Exception e) {
                String errorMessage = e.getMessage();
                ctx.status(500).result(String.format("{\"message\": \"%s\"}", errorMessage));
            }
        });

        //register
        server.post("/user", ctx -> {
            try {
                UserService.RegisterRequest req = gson.fromJson(ctx.body(), UserService.RegisterRequest.class);
                var res = userService.register(req);
                ctx.status(200).json(gson.toJson(res));
            } catch (IllegalArgumentException ex) {
                String errorMessage = "Error: bad request";
                ctx.status(400).result(String.format("{\"message\": \"%s\"}", errorMessage));
            } catch (DataAccessException ex) {
                if ("already taken".equals(ex.getMessage())) {
                    String errorMessage = "Error: already taken";
                    ctx.status(403).result(String.format("{\"message\": \"%s\"}", errorMessage));
                }
                else {
                    String errorMessage = ex.getMessage();
                    ctx.status(500).result(String.format("{\"message\": \"%s\"}", errorMessage));
                }
            } catch (Exception ex) {
                String errorMessage = ex.getMessage();
                ctx.status(500).result(String.format("{\"message\": \"%s\"}", errorMessage));
            }
        });

        //login
        server.post("/session", ctx -> {
            try {
                UserService.LoginRequest req = gson.fromJson(ctx.body(), UserService.LoginRequest.class);
                var res = userService.login(req);
                ctx.status(200).json(gson.toJson(res));
            } catch (IllegalArgumentException ex) {
                String errorMessage = "Error: bad request";
                ctx.status(400).result(String.format("{\"message\": \"%s\"}", errorMessage));
            } catch (DataAccessException ex) {
                String errorMessage = "Error: unauthorized";
                ctx.status(401).result(String.format("{\"message\": \"%s\"}", errorMessage));
            } catch (Exception ex) {
                String errorMessage = ex.getMessage();
                ctx.status(500).result(String.format("{\"message\": \"%s\"}", errorMessage));
            }
        });

        //logout
        server.delete("/session", ctx -> {
            String token = ctx.header("authorization");
            try {
                userService.logout(token);
                ctx.status(200).result("{}");
            } catch (IllegalArgumentException | DataAccessException ex) {
                String errorMessage = "Error: unauthorized";
                ctx.status(401).result(String.format("{\"message\": \"%s\"}", errorMessage));
            } catch (Exception ex) {
                String errorMessage = ex.getMessage();
                ctx.status(500).result(String.format("{\"message\": \"%s\"}", errorMessage));
            }
        });

        //listGames
        server.get("/game", ctx -> {
            String token =  ctx.header("authorization");
            try {
                var res = gameService.listGames(token);
                ctx.status(200).json(gson.toJson(res));
            } catch (IllegalArgumentException ex) {
                String errorMessage = "Error: unauthorized";
                ctx.status(401).result(String.format("{\"message\": \"%s\"}", errorMessage));
            } catch (DataAccessException ex) {
                if ("unauthorized".equals(ex.getMessage())) {
                    String errorMessage = "Error: unauthorized";
                    ctx.status(401).result(String.format("{\"message\": \"%s\"}", errorMessage));
                }
                else {
                    String errorMessage = ex.getMessage();
                    ctx.status(500).result(String.format("{\"message\": \"%s\"}", errorMessage));
                }
            } catch (Exception ex) {
                String errorMessage = ex.getMessage();
                ctx.status(500).result(String.format("{\"message\": \"%s\"}", errorMessage));
            }
        });

        //createGame
        server.post("/game", ctx -> {
            String token = ctx.header("authorization");
            try {
                var req = gson.fromJson(ctx.body(), GameService.CreateGameRequest.class);
                var res = gameService.createGame(token, req);
                ctx.status(200).json(gson.toJson(res));
            } catch (IllegalArgumentException ex) {
                String errorMessage =  "Error: bad request";
                ctx.status(400).result(String.format("{\"message\": \"%s\"}", errorMessage));
            } catch (DataAccessException ex) {
                if ("unauthorized".equals(ex.getMessage())) {
                    String errorMessage = "Error: unauthorized";
                    ctx.status(401).result(String.format("{\"message\": \"%s\"}", errorMessage));
                }
                else {
                    String errorMessage = ex.getMessage();
                    ctx.status(500).result(String.format("{\"message\": \"%s\"}", errorMessage));
                }
            } catch (Exception ex) {
                String errorMessage = ex.getMessage();
                ctx.status(500).result(String.format("{\"message\": \"%s\"}", errorMessage));
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
                String  errorMessage = "Error: bad request";
                ctx.status(400).result(String.format("{\"message\": \"%s\"}", errorMessage));
            } catch (DataAccessException ex) {
                if ("unauthorized".equals(ex.getMessage())) {
                    String errorMessage = "Error: unauthorized";
                    ctx.status(401).result(String.format("{\"message\": \"%s\"}", errorMessage));
                }
                else if ("already taken".equals(ex.getMessage())) {
                    String errorMessage = "Error: already taken";
                    ctx.status(403).result(String.format("{\"message\": \"%s\"}", errorMessage));
                }
                else {
                    String errorMessage = ex.getMessage();
                    ctx.status(500).result(String.format("{\"message\": \"%s\"}", errorMessage));
                }
            } catch (Exception ex) {
                String errorMessage = ex.getMessage();
                ctx.status(500).result(String.format("{\"message\": \"%s\"}", errorMessage));
            }
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
