package websocket;

import com.google.gson.*;
import com.google.gson.GsonBuilder;
import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import dataaccess.MemoryDataAccess;
import dataaccess.MySQLDataAccess;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.websocket.*;
import model.Auth;
import model.Game;
import websocket.commands.UserGameCommand;
import websocket.messages.ErrorMessage;
import websocket.messages.LoadGameMessage;
import websocket.messages.NotificationMessage;

public class GameWebSocketEndpoint {
    private static final Gson gson = new GsonBuilder().create();
    private static final ConcurrentHashMap<Integer, GameConnections> games = new ConcurrentHashMap<>();
    private static final DataAccess dao;
    static {
        DataAccess tmp;
        try {
            tmp = new MySQLDataAccess();
        } catch (DataAccessException ex) {
            tmp = new MemoryDataAccess();
        }
        dao = tmp;
    }

    @OnOpen
    public void onOpen(Session session) {}

    @OnMessage
    public void onMessage(Session session, String msg) {
        JsonObject root;
        try {
            root = JsonParser.parseString(msg).getAsJsonObject();
        } catch (JsonParseException | IllegalStateException ex) {
            sendError(session, "error: invalid JSON");
            return;
        }

        JsonElement ctEl = root.get("commandType");
        if (ctEl == null) {
            sendError(session, "error: missing commandType");
            return;
        }

        String ctStr =  ctEl.getAsString();
        switch (ctStr) {}
    }

    @OnClose
    public void onClose(Session session, CloseReason reason) {
        games.values().forEach(gc -> gc.removeSession(session));
    }

    @OnError
    public void onError(Session session, Throwable thr) {}

    private void handleConnect(Session session, UserGameCommand cmd) {
        if (cmd == null) {
            sendError(session, "error: invalid command");
            return;
        }
        Integer gameID = cmd.getGameID();
        String token = cmd.getAuthToken();

        try {
            Auth auth = dao.getAuth(token);
            if (auth == null) {
                sendError(session, "error: invalid auth token");
                return;
            }

            Game game = dao.getGame(gameID);
            if (game == null) {
                sendError(session, "error: game not found");
                return;
            }

            GameConnections gc = games.computeIfAbsent(gameID, id -> new GameConnections(gameID));
            gc.addSession(session, auth.username());

            LoadGameMessage load = new LoadGameMessage(GameDTO.fromModel(game));
            sendJson(session, load);

            String side = gc.getSideForUsername(auth.username(), game);
            String notifText = auth.username() + " connected as " + side;
            gc.broadcastNotificationExcept(new NotificationMessage(notifText), session);
        } catch (DataAccessException ex) {
            sendError(session, "error: server data error");
        }
    }

    private void sendError(Session session, String msg) {
        try {
            ErrorMessage em = new ErrorMessage(msg);
            sendJson(session, em);
        } catch (Exception ex) {}
    }

    private void sendJson(Session session, ErrorMessage msg) {
        try {
            String json = gson.toJson(msg);
            session.getBasicRemote().sendText(json);
        } catch (IOException ex) {}
    }
}
