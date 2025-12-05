package server.websocket;

import com.google.gson.Gson;
import model.Game;
import websocket.messages.NotificationMessage;
import websocket.messages.ServerMessage;
import io.javalin.websocket.WsContext;

import java.util.concurrent.ConcurrentHashMap;

public class GameConnections {
    private static final Gson GSON = new Gson();
    private final int gameID;
    private final ConcurrentHashMap<WsContext, String> sessions = new ConcurrentHashMap<>();

    public GameConnections(int gameID) {
        this.gameID = gameID;
    }

    public void addSession(WsContext session, String username) {
        sessions.put(session, username);
    }

    public void removeSession(WsContext session) {
        sessions.remove(session);
    }

    public void removeSession(WsContext session, String username) {
        sessions.remove(session, username);
    }

    public void broadcastJson(ServerMessage msg) {
        String json = GSON.toJson(msg);
        for (WsContext s : sessions.keySet()) {
            try {
                s.send(json);
            } catch (Exception ex) {}
        }
    }

    public void broadcastJsonExcept(ServerMessage msg, WsContext except) {
        String json = GSON.toJson(msg);
        for (WsContext s : sessions.keySet()) {
            if (s.equals(except)) {continue;}
            try {
                s.send(json);
            } catch (Exception ex) {}
        }
    }

    public void broadcastNotification(NotificationMessage msg) {
        broadcastJson(msg);
    }

    public void broadcastNotificationExcept(NotificationMessage notificationMessage, WsContext session) {
        broadcastJsonExcept(notificationMessage, session);
    }


    public String getSideForUsername(String username, Game game) {
        if (username == null) {
            return "Observer";
        }
        if (username.equals(game.whiteUsername())) {
            return "WHITE";
        }
        if (username.equals(game.blackUsername())) {
            return "BLACK";
        }
        return "Observer";
    }
}
