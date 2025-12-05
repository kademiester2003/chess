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
import java.util.concurrent.CompletableFuture;

public class ChessWS implements WebSocket.Listener {
    private final URI uri;
    private WebSocket socket;
    private final Gson gson = new Gson();

    private ChessGame currentGame = null;
    private ChessGame.TeamColor perspective = ChessGame.TeamColor.WHITE;

    // Pending connect info (if connect requested before socket open)
    private String pendingToken = null;
    private Integer pendingGameId = null;

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

        // If CONNECT was requested earlier, send it now.
        if (pendingToken != null && pendingGameId != null) {
            sendConnect(pendingToken, pendingGameId);
        }
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

    /**
     * Send a CONNECT command. If socket isn't open yet, the command is queued and will be sent
     * automatically from onOpen().
     */
    public void sendConnect(String token, int gameID) {
        // Save pending in case onOpen hasn't fired yet
        this.pendingToken = token;
        this.pendingGameId = gameID;

        if (socket != null) {
            sendJson(new UserGameCommand(UserGameCommand.CommandType.CONNECT, token, gameID));
            // we consider it sent (pending cleared)
            this.pendingToken = null;
            this.pendingGameId = null;
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

    /**
     * Send JSON over the websocket. This function logs the JSON being sent and will also
     * report send success/failure asynchronously so the developer (you) can see whether
     * the LEAVE/RESIGN messages actually left the client.
     */
    private void sendJson(Object obj) {
        if (socket == null) {
            System.err.println("[ws] cannot send - socket is null. JSON: " + gson.toJson(obj));
            return;
        }

        String json = gson.toJson(obj);
        System.out.println("[ws ->] " + json);

        try {
            CompletableFuture<WebSocket> cf = socket.sendText(json, true);
            cf.whenComplete((ws, ex) -> {
                if (ex != null) {
                    System.err.println("[ws] send failed: " + ex.getMessage());
                } else {
                    System.out.println("[ws] send completed.");
                }
            });
        } catch (Exception e) {
            System.err.println("[ws] failed to send json: " + e.getMessage());
        }
    }

    /**
     * Close the websocket if you want to explicitly close the connection from the client.
     */
    public void close() {
        if (socket != null) {
            try {
                socket.sendClose(WebSocket.NORMAL_CLOSURE, "client closing").join();
            } catch (Exception e) {
                // ignore
            } finally {
                socket = null;
                System.out.println("[ws] closed client-side");
            }
        }
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