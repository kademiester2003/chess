package websocket;

import com.google.gson.Gson;
import jakarta.websocket.Session;

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

    public void removeSession(Session session, String username) {
        sessions.remove(session, username);
    }
}
