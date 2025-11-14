package client;

import com.google.gson.Gson;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

public class ServerFacade {
    private final String baseUrl;
    private final HttpClient http;
    private final Gson gson = new Gson();

    public ServerFacade(String baseUrl, int port) {
        this.baseUrl = String.format("http://%s:%d/", baseUrl, port);
        this.http = HttpClient.newHttpClient();
    }

    //data classes interface
    public static class RegisterRequest {public String username; public String password; public String email;}
    public static class LoginRequest {public String username; public String password;}
    public static class CreateGameRequest {public String gameName; public CreateGameRequest(String gameName) { this.gameName = gameName;}}
    public static class JoinGameRequest {public String team; public int gameID; public JoinGameRequest(String team, int gameID) { this.team = team; this.gameID = gameID;}}

    public static class RegisterResponse {public String authToken; public String username;}
    public static class LoginResponse {public String authToken; public String username;}
    public static class CreateGameResponse {public int gameID; public String message;}
    public static class GenericResponse {public String message;}
    public static class GameEntry {public int gameID; public String whiteUsername; public String blackUsername; public String gameName;}
    public static class ListGamesResponse {public GameEntry[] games;}


    private HttpResponse<String> doPost(String path, String json, String authToken) throws IOException, InterruptedException {
        var builder = HttpRequest.newBuilder(URI.create(baseUrl + path)).POST(HttpRequest.BodyPublishers.ofString(json)).header("Content-Type", "application/json");
        if (authToken != null) {
            builder.header("authorization", authToken);
        }
        var req =  builder.build();
        return http.send(req, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> doGet(String path, String authToken) throws IOException, InterruptedException {
        var builder = HttpRequest.newBuilder(URI.create(baseUrl + path)).GET();
        if (authToken != null) {
            builder.header("authorization", authToken);
        }
        var req =  builder.build();
        return http.send(req, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> doDelete(String path, String authToken) throws IOException, InterruptedException {
        var builder = HttpRequest.newBuilder(URI.create(baseUrl + path)).DELETE();
        if (authToken != null) {
            builder.header("authorization", authToken);
        }
        var req =  builder.build();
        return http.send(req, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> doPut(String path, String json, String authToken) throws IOException, InterruptedException {
        var builder = HttpRequest.newBuilder(URI.create(baseUrl + path)).PUT(HttpRequest.BodyPublishers.ofString(json)).header("Content-Type", "application/json");
        if (authToken != null) {
            builder.header("authorization", authToken);
        }
        var req =  builder.build();
        return http.send(req, HttpResponse.BodyHandlers.ofString());
    }

    public RegisterResponse register(String username, String password, String email) throws IOException, InterruptedException {
        RegisterRequest req = new RegisterRequest();
        req.username = username;
        req.password = password;
        req.email = email;
        var resp = doPost("/user", gson.toJson(req), null);
        if (resp.statusCode() == 200) {
            return  gson.fromJson(resp.body(), RegisterResponse.class);
        } else {
            GenericResponse gr = safeParse(resp.body(), GenericResponse.class);
            RegisterResponse r = new RegisterResponse();
            r.authToken = null;
            r.username = (gr != null ? gr.message : "Error: server returned " + resp.statusCode());
            return r;
        }
    }

    public LoginResponse login(String username, String password) throws IOException, InterruptedException {
        LoginRequest req = new LoginRequest();
        req.username = username;
        req.password = password;
        var resp = doPost("/session", gson.toJson(req), null);
        if (resp.statusCode() == 200) {
            return gson.fromJson(resp.body(), LoginResponse.class);
        } else {
            GenericResponse gr = safeParse(resp.body(), GenericResponse.class);
            LoginResponse r = new LoginResponse();
            r.authToken = null;
            r.username = (gr != null ? gr.message : "Error: server returned " + resp.statusCode());
            return r;
        }
    }

    public GenericResponse logout(String authToken) throws IOException, InterruptedException {
        var resp = doDelete("/session", authToken);
        if (resp.statusCode() == 200) {
            return  gson.fromJson(resp.body(), GenericResponse.class);
        } else {
            GenericResponse gr = safeParse(resp.body(), GenericResponse.class);
            if (gr == null) {
                gr = new GenericResponse();
            }
            gr.message = gr.message == null ? ("Error: server returned " + resp.statusCode()) : gr.message;
            return gr;
        }
    }

    public CreateGameResponse createGame(String gameName, String authToken) throws IOException, InterruptedException {
        var req = new CreateGameRequest(gameName);
        var resp = doPost("/game", gson.toJson(req), authToken);
        if (resp.statusCode() == 200) {
            return gson.fromJson(resp.body(), CreateGameResponse.class);
        } else {
            GenericResponse gr = safeParse(resp.body(), GenericResponse.class);
            CreateGameResponse r = new CreateGameResponse();
            r.message = (gr != null ? gr.message : "Error: server returned " + resp.statusCode());
            r.gameID = -1;
            return r;
        }
    }

    public List<GameEntry> listGames(String authToken) throws IOException, InterruptedException {
        var resp = doGet("/games", authToken);
        if (resp.statusCode() == 200) {
            try {
                ListGamesResponse lr = gson.fromJson(resp.body(), ListGamesResponse.class);
                if (lr != null && lr.games != null) {
                    return List.of(lr.games);
                }
            } catch (Exception e) {}
            try {
                GameEntry[] arr = gson.fromJson(resp.body(), GameEntry[].class);
                return List.of(arr);
            } catch (Exception e) {}
        }
        return List.of();
    }

    public GenericResponse joinGame(String team, int gameID, String authToken) throws IOException, InterruptedException {
        JoinGameRequest req = new JoinGameRequest(team, gameID);
        var resp = doPost("/game", gson.toJson(req), authToken);
        if (resp.statusCode() == 200) {
            return safeParse(resp.body(), GenericResponse.class);
        } else {
            var putResp = doPut("/game", gson.toJson(req), authToken);
            if (putResp.statusCode() == 200) {
                return safeParse(putResp.body(), GenericResponse.class);
            }
            GenericResponse gr = safeParse(putResp.body(), GenericResponse.class);
            if (gr == null) {
                gr = new GenericResponse();
            }
            gr.message = gr.message == null ? ("Error: server returned " + resp.statusCode()) : gr.message;
            return gr;
        }
    }

    private <T> T safeParse(String body, Class<T> clazz) {
        try {
            return gson.fromJson(body, clazz);
        } catch (Exception ex) {
            return null;
        }
    }
}
