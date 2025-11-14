package client;

import com.google.gson.Gson;
import java.io.*;
import java.net.*;

public class ServerFacade {
    private final String serverUrl;
    private final Gson gson = new Gson();

    public ServerFacade(String host, String port) {
        this.serverUrl = "http://" + host + ":" + port;
    }

    //data classes interface
    public static class RegisterRequest {public String username; public String password; public String email;}
    public static class LoginRequest {public String username; public String password;}
    public static class CreateGameRequest {public String gameName; public CreateGameRequest(String gameName) { this.gameName = gameName;}}
    public static class JoinGameRequest {public String team; public int gameID; public JoinGameRequest(String team, int gameID) { this.team = team; this.gameID = gameID;}}

    public static class RegisterResponse {public String authToken; public String username; public String message;}
    public static class LoginResponse {public String authToken; public String username; public String message;}
    public static class LogoutResponse {public String message;}
    public static class CreateGameResponse {public int gameID; public String message;}
    public static class JoinGameResponse {public String message;}
    public static class GameEntry {public int gameID; public String whiteUsername; public String blackUsername; public String gameName;}
    public static class ListGamesResponse {public GameEntry[] games; public String message;}


    public RegisterResponse register(RegisterRequest request) throws IOException {
        HttpURLConnection connection = makeRequest("POST", "/user", request, null);
        return readResponse(connection, RegisterResponse.class);
    }

    public LoginResponse login(LoginRequest request) throws IOException {
        HttpURLConnection connection = makeRequest("POST", "/session", request, null);
        return readResponse(connection, LoginResponse.class);
    }

   public LogoutResponse logout(String authToken) throws IOException {
        HttpURLConnection connection = makeRequest("DELETE", "/session", null, authToken);
        return readResponse(connection, LogoutResponse.class);
    }

    public CreateGameResponse createGame(CreateGameRequest request, String authToken) throws IOException {
        HttpURLConnection connection = makeRequest("POST", "/game", request, authToken);
        return readResponse(connection, CreateGameResponse.class);
    }

    public ListGamesResponse listGames(String authToken) throws IOException {
        HttpURLConnection connection = makeRequest("GET", "/game", null, authToken);
        return readResponse(connection, ListGamesResponse.class);
    }

    public JoinGameResponse joinGame(JoinGameRequest request, String authToken) throws IOException {
        HttpURLConnection connection = makeRequest("PUT", "/game", request, authToken);
        return readResponse(connection, JoinGameResponse.class);
    }

    private HttpURLConnection makeRequest(
            String method,
            String path,
            Object body,
            String authToken
    ) throws IOException {

        //System.out.println("URL = " + serverUrl + path);
        URI uri = URI.create(serverUrl + path);
        URL url = uri.toURL();

        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod(method);
        connection.setDoOutput(body != null);
        connection.setRequestProperty("Accept", "application/json");

        if (authToken != null) {
            connection.setRequestProperty("Authorization", authToken);
        }

        if (body != null) {
            connection.setRequestProperty("Content-Type", "application/json");
            String json = gson.toJson(body);
            //OutputStream os = connection.getOutputStream();
            try (OutputStream os = connection.getOutputStream()) {
                os.write(json.getBytes());
            }
        }

        return connection;
    }

    private <T> T readResponse(HttpURLConnection connection, Class<T> responseClass) throws IOException {
        InputStream stream;

        if (connection.getResponseCode() / 100 == 2) {
            stream = connection.getInputStream();
        } else {
            stream = connection.getErrorStream();
            if (stream == null) {
                try {
                    T resp = responseClass.getDeclaredConstructor().newInstance();
                    responseClass.getField("message").set(resp,
                            "Error: HTTP " + connection.getResponseCode());
                    return resp;
                } catch (Exception e) {
                    throw new IOException("HTTP error " + connection.getResponseCode());
                }
            }
        }

        try (InputStreamReader reader = new InputStreamReader(stream)) {
            return gson.fromJson(reader, responseClass);
        }
    }
}