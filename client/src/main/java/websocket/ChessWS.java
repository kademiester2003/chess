package websocket;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import websocket.commands.MakeMoveCommand;
import websocket.commands.UserGameCommand;
import websocket.messages.LoadGameMessage;
import websocket.messages.ErrorMessage;
import websocket.messages.NotificationMessage;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.net.http.WebSocket.Listener;
import java.util.concurrent.CompletionStage;

public class ChessWS implements WebSocket.Listener {
    private final URI uri;
    private WebSocket socket;
    private final Gson gson = new Gson();

    public ChessWS(String url) {
        this.uri = URI.create(url);
    }

    // Connect using Java's built-in WebSocket client
    public void connect() {
        this.socket = HttpClient.newHttpClient()
                .newWebSocketBuilder()
                .buildAsync(uri, this)
                .join();
        System.out.println("[ws] connected");
    }

    @Override
    public void onOpen(WebSocket ws) {
        Listener.super.onOpen(ws);
        System.out.println("[ws] open");
    }

    @Override
    public CompletionStage<?> onText(WebSocket ws, CharSequence data, boolean last) {
        String json = data.toString();
        handleMessage(json);
        return Listener.super.onText(ws, data, last);
    }

    private void handleMessage(String json) {
        try {
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            String type = obj.get("serverMessageType").getAsString();

            switch (type) {
                case "LOAD_GAME" -> {
                    LoadGameMessage m = gson.fromJson(json, LoadGameMessage.class);
                    System.out.println("[LOAD_GAME] gameID=" + m.getGame().gameID);
                }
                case "ERROR" -> {
                    ErrorMessage e = gson.fromJson(json, ErrorMessage.class);
                    System.err.println("[ERROR] " + e.getErrorMessage());
                }
                case "NOTIFICATION" -> {
                    NotificationMessage n = gson.fromJson(json, NotificationMessage.class);
                    System.out.println("[NOTIFICATION] " + n.getMessage());
                }
                default -> System.out.println("[UNKNOWN] " + json);
            }
        } catch (Exception ex) {
            System.err.println("Failed to parse: " + ex.getMessage());
            System.err.println("Raw: " + json);
        }
    }

    public void sendConnect(String token, int gameID) {
        UserGameCommand cmd =
                new UserGameCommand(UserGameCommand.CommandType.CONNECT, token, gameID);
        sendJson(cmd);
    }

    public void sendLeave(String token, int gameID) {
        UserGameCommand cmd =
                new UserGameCommand(UserGameCommand.CommandType.LEAVE, token, gameID);
        sendJson(cmd);
    }

    public void sendResign(String token, int gameID) {
        UserGameCommand cmd =
                new UserGameCommand(UserGameCommand.CommandType.RESIGN, token, gameID);
        sendJson(cmd);
    }

    public void sendMakeMove(String token, int gameID, MakeMoveCommand.Move move) {
        MakeMoveCommand cmd =
                new MakeMoveCommand(MakeMoveCommand.CommandType.MAKE_MOVE, token, gameID, move);
        sendJson(cmd);
    }

    private void sendJson(Object obj) {
        if (socket == null) return;
        String json = gson.toJson(obj);
        socket.sendText(json, true);
    }

    public boolean isConnected() {
        return socket != null;
    }

    public void close() {
        if (socket != null) {
            socket.sendClose(WebSocket.NORMAL_CLOSURE, "bye");
        }
    }
}
