package client;

import chess.ChessGame;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

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

    private ChessGame currentGame = null;
    private ChessGame.TeamColor perspective = ChessGame.TeamColor.WHITE;

    public ChessWS(String url) {
        this.uri = URI.create(url);
    }

    public void connect() {
        this.socket = HttpClient.newHttpClient().newWebSocketBuilder().buildAsync(uri, this).join();
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

                    this.currentGame = m.getGame().game();

                    if (m.getGame().whiteUsername() != null &&
                            m.getGame().whiteUsername().equalsIgnoreCase(m.getGame().currentUser())) {
                        this.perspective = ChessGame.TeamColor.WHITE;
                    } else if (m.getGame().blackUsername() != null &&
                            m.getGame().blackUsername().equalsIgnoreCase(m.getGame().currentUser())) {
                        this.perspective = ChessGame.TeamColor.BLACK;
                    }

                    System.out.println("[LOAD_GAME] Updated local game state.");
                    BoardDrawer.drawBoard(this.currentGame, this.perspective);
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
            System.err.println("Failed to parse WS message: " + ex.getMessage());
            System.err.println("Raw: " + json);
        }
    }

    public void sendConnect(String token, int gameID) {
        sendJson(new UserGameCommand(UserGameCommand.CommandType.CONNECT, token, gameID));
    }

    public void sendLeave(String token, int gameID) {
        sendJson(new UserGameCommand(UserGameCommand.CommandType.LEAVE, token, gameID));
    }

    public void sendResign(String token, int gameID) {
        sendJson(new UserGameCommand(UserGameCommand.CommandType.RESIGN, token, gameID));
    }

    public void sendMakeMove(String token, int gameID, UserGameCommand.Move move) {
        sendJson(new UserGameCommand(UserGameCommand.CommandType.MAKE_MOVE, token, gameID, move));
    }

    private void sendJson(Object obj) {
        if (socket == null) {return;}
        socket.sendText(gson.toJson(obj), true);
    }

    public boolean isConnected() {
        return socket != null;
    }

    public ChessGame getCurrentGame() {
        return currentGame;
    }

    public ChessGame.TeamColor getPerspective() {
        return perspective;
    }
}