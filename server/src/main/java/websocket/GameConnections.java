package websocket;

import com.google.gson.Gson;
import jakarta.websocket.Session;
import model.Game;
import websocket.messages.NotificationMessage;
import websocket.messages.ServerMessage;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class GameConnections {
    private static final Gson gson = new Gson();
    private final int gameID;
    private final ConcurrentHashMap<Session, String> sessions = new ConcurrentHashMap<>();

    public GameConnections(int gameID) {
        this.gameID = gameID;
    }

    public void addSession(Session session, String username) {
        sessions.put(session, username);
    }

    public void removeSession(Session session) {
        String removed = sessions.remove(session);
    }

    public void removeSession(Session session, String username) {
        sessions.remove(session, username);
    }
    
    //public void removeSession(Optional<Session> s) {}

    public void broadcastJson(ServerMessage msg) {
        String json = gson.toJson(msg);
        for (Session s : sessions.keySet()) {
            try {
                s.getBasicRemote().sendText(json);
            } catch (IOException ex) {}
        }
    }

    public void broadcastJsonExcept(ServerMessage msg, Session except) {
        String json = gson.toJson(msg);
        for (Session s : sessions.keySet()) {
            if (s.equals(except)) {continue;}
            try {
                s.getBasicRemote().sendText(json);
            } catch (IOException ex) {}
        }
    }

    public void broadcastNotification(NotificationMessage msg) {
        broadcastJson(msg);
    }

    public void broadcastNotificationExcept(NotificationMessage notificationMessage, Session session) {
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
