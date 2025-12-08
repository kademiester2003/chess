package client;

import chess.ChessGame;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import websocket.commands.UserGameCommand;
import websocket.messages.LoadGameMessage;
import websocket.messages.ErrorMessage;
import websocket.messages.NotificationMessage;

import model.Game;

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

    private String localUserToken = null;
    private String localUsername = null;

    private String pendingToken = null;
    private Integer pendingGameId = null;

    public ChessWS(String url) {
        this.uri = URI.create(url);
    }

    public void connect() {
        this.socket = HttpClient.newHttpClient()
                .newWebSocketBuilder()
                .buildAsync(uri, this)
                .join();
        System.out.println("[ws] connected");
    }

    public void setLocalUserToken(String token) {
        this.localUserToken = token;
    }

    public void setLocalUsername(String username) {
        this.localUsername = username;
    }

    @Override
    public void onOpen(WebSocket ws) {
        Listener.super.onOpen(ws);
        System.out.println("[ws] open");

        if (pendingToken != null && pendingGameId != null) {
            sendConnect(pendingToken, pendingGameId);
        }
    }

    @Override
    public CompletionStage<?> onText(WebSocket ws, CharSequence data, boolean last) {
        handleMessage(data.toString());
        return Listener.super.onText(ws, data, last);
    }

    private void handleMessage(String json) {
        try {
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            String type = obj.get("serverMessageType").getAsString();

            switch (type) {

                case "LOAD_GAME" -> {
                    LoadGameMessage m = gson.fromJson(json, LoadGameMessage.class);
                    Game dbGame = m.getGame();

                    this.currentGame = dbGame.game();

                    determinePerspective(dbGame);

                    System.out.println("[LOAD_GAME] Game updated. Redrawing...");
                    BoardDrawer.drawBoard(currentGame, perspective);
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

    private void determinePerspective(Game dbGame) {
        if (localUsername == null) {return;}

        String white = dbGame.whiteUsername();
        String black = dbGame.blackUsername();

        if (localUsername.equals(white)) {
            perspective = ChessGame.TeamColor.WHITE;
        } else if (localUsername.equals(black)) {
            perspective = ChessGame.TeamColor.BLACK;
        }
    }

    public void sendConnect(String token, int gameID) {
        this.pendingToken = token;
        this.pendingGameId = gameID;

        if (socket != null) {
            sendJson(new UserGameCommand(UserGameCommand.CommandType.CONNECT, token, gameID));
            pendingToken = null;
            pendingGameId = null;
        }
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
        if (socket == null) {
            System.err.println("[ws] cannot send - socket is null");
            return;
        }

        String json = gson.toJson(obj);
        //System.out.println("[ws ->] " + json);

        socket.sendText(json, true).whenComplete((ws, ex) -> {
                    if (ex != null) {System.err.println("[ws] send failed: " + ex.getMessage());}
                });
    }

    public ChessGame getCurrentGame() {
        return currentGame;
    }

    public ChessGame.TeamColor getPerspective() {
        return perspective;
    }

    public boolean isConnected() {
        return socket != null;
    }

    public void close() {
        if (socket != null) {
            try {
                socket.sendClose(WebSocket.NORMAL_CLOSURE, "client closing").join();
            } catch (Exception ignored) {}
            socket = null;
            System.out.println("[ws] closed client-side");
        }
    }
}