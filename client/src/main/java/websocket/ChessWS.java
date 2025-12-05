package websocket;

import com.google.gson.Gson;
import websocket.commands.MakeMoveCommand;
import websocket.messages.ServerMessage;
import websocket.messages.LoadGameMessage;
import websocket.messages.ErrorMessage;
import websocket.messages.NotificationMessage;

import jakarta.websocket.*;
import java.net.URI;

@ClientEndpoint
public class ChessWS {
    private final URI uri;
    private Session session;
    private final Gson gson = new Gson();

    public ChessWS(String uri) {
        this.uri = URI.create(uri);
    }

    public void connect() throws Exception {
        WebSocketContainer container = ContainerProvider.getWebSocketContainer();
        container.connectToServer(this, uri);
    }

    @OnOpen
    public void onOpen(Session session) {
        this.session = session;
        System.out.println("[ws] connected");
    }

    @OnMessage
    public void onMessage(String message) {
        try {
            ServerMessage base = gson.fromJson(message, ServerMessage.class);
            switch (base.getServerMessageType()) {
                case LOAD_GAME -> {
                    LoadGameMessage m = gson.fromJson(message, LoadGameMessage.class);
                    System.out.println("[LOAD_GAME] gameID=" + m.getGame().gameID);
                }
                case ERROR -> {
                    ErrorMessage e = gson.fromJson(message, ErrorMessage.class);
                    System.err.println("[ERROR] " + e.getErrorMessage());
                }
                case NOTIFICATION -> {
                    NotificationMessage n = gson.fromJson(message, NotificationMessage.class);
                    System.out.println("[NOTIFICATION] " + n.getMessage());
                }
            }
        } catch (Exception ex) {
            System.err.println("failed to parse server message: " + ex.getMessage());
        }
    }

    @OnClose
    public void onClose(CloseReason reason) {
        System.out.println("[ws] closed: " + reason);
    }

    public void sendConnect(String token, int gameID) {
        websocket.commands.UserGameCommand cmd =
                new websocket.commands.UserGameCommand(websocket.commands.UserGameCommand.CommandType.CONNECT, token, gameID);
        sendJson(cmd);
    }

    public void sendLeave(String token, int gameID) {
        websocket.commands.UserGameCommand cmd =
                new websocket.commands.UserGameCommand(websocket.commands.UserGameCommand.CommandType.LEAVE, token, gameID);
        sendJson(cmd);
    }

    public void sendResign(String token, int gameID) {
        websocket.commands.UserGameCommand cmd =
                new websocket.commands.UserGameCommand(websocket.commands.UserGameCommand.CommandType.RESIGN, token, gameID);
        sendJson(cmd);
    }

    public void sendMakeMove(String token, int gameID, MakeMoveCommand.Move move) {
        MakeMoveCommand cmd = new MakeMoveCommand(MakeMoveCommand.CommandType.MAKE_MOVE, token, gameID, move);
        sendJson(cmd);
    }

    private void sendJson(Object obj) {
        try {
            String json = gson.toJson(obj);
            session.getBasicRemote().sendText(json);
        } catch (Exception ex) {
            System.err.println("failed to send ws message: " + ex.getMessage());
        }
    }

    public boolean isConnected() {
        return session != null && session.isOpen();
    }

    public void close() throws Exception {
        if (session != null) session.close();
    }
}
