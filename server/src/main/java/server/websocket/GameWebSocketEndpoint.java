package server.websocket;

import chess.*;
import com.google.gson.*;
import com.google.gson.GsonBuilder;
import dataaccess.DataAccess;
import dataaccess.DataAccessException;

import java.util.concurrent.ConcurrentHashMap;

import io.javalin.websocket.*;
import model.Auth;
import model.Game;
import websocket.commands.MakeMoveCommand;
import websocket.commands.UserGameCommand;
import websocket.messages.ErrorMessage;
import websocket.messages.LoadGameMessage;
import websocket.messages.NotificationMessage;
import websocket.messages.ServerMessage;

public class GameWebSocketEndpoint {
    private static final Gson GSON = new GsonBuilder().create();
    private static final ConcurrentHashMap<Integer, GameConnections> Games = new ConcurrentHashMap<>();
    private final DataAccess dao;

    public GameWebSocketEndpoint(DataAccess dao) {
        this.dao = dao;
    }

    public void onConnect(WsConnectContext ctx) {}

    public void onMessage(WsMessageContext ctx, String msg) {
        JsonObject root;
        try {
            root = JsonParser.parseString(msg).getAsJsonObject();
        } catch (JsonParseException | IllegalStateException ex) {
            sendError(ctx, "error: invalid JSON");
            return;
        }

        String type = root.has("commandType") ? root.get("commandType").getAsString() : null;
        if (type == null) {
            sendError(ctx, "error: missing commandType");
            return;
        }

        switch (type) {
            case "CONNECT" -> handleConnect(ctx, GSON.fromJson(root, UserGameCommand.class));
            case "LEAVE" -> handleLeave(ctx, GSON.fromJson(root, UserGameCommand.class));
            case "RESIGN" -> handleResign(ctx, GSON.fromJson(root, UserGameCommand.class));
            case "MAKE_MOVE" -> handleMakeMove(ctx, GSON.fromJson(root, MakeMoveCommand.class));
            default -> sendError(ctx, "error: unknown command");
        }
    }

    public void onClose(WsCloseContext ctx) {}

    public void onError(WsErrorContext ctx, Throwable thr) {}

    private boolean requireCommand(Object cmd, WsContext ctx) {
        if (cmd == null) {
            sendError(ctx, "error: invalid command");
            return true;
        }
        return false;
    }

    private Auth requireAuth(String token, WsContext ctx) throws DataAccessException {
        Auth auth = dao.getAuth(token);
        if (auth == null) {
            sendError(ctx, "error: invalid auth token");
            return null;
        }
        return auth;
    }

    private Game requireGame(Integer gameID, WsContext ctx) throws DataAccessException {
        Game game = dao.getGame(gameID);
        if (game == null) {
            sendError(ctx, "error: game not found");
            return null;
        }
        return game;
    }

    private ChessGame.TeamColor getPlayerTeam(Game model, String username, WsContext ctx) {
        if (username.equals(model.whiteUsername())) {
            return ChessGame.TeamColor.WHITE;
        }
        if (username.equals(model.blackUsername())) {
            return ChessGame.TeamColor.BLACK;
        }
        sendError(ctx, "error: you are not a player in this game");
        return null;
    }

    private void handleConnect(WsContext ctx, UserGameCommand cmd) {
        if (requireCommand(cmd, ctx)) {return;}

        Integer gameID = cmd.getGameID();
        String token = cmd.getAuthToken();

        try {
            Auth auth = requireAuth(token, ctx);
            if (auth == null) {return;}

            Game game = requireGame(gameID, ctx);
            if (game == null) {return;}

            GameConnections gc = Games.compute(gameID, (id, existing) ->
                    existing != null ? existing : new GameConnections(gameID));

            gc.addSession(ctx, auth.username());

            sendJson(ctx, new LoadGameMessage(game));

            String side = gc.getSideForUsername(auth.username(), game);
            gc.broadcastNotificationExcept(
                    new NotificationMessage(auth.username() + "connected as" + side),
                    ctx);

        } catch (DataAccessException ex) {
            sendError(ctx, "error: server data error");
        }
    }

    private void handleLeave(WsContext ctx, UserGameCommand cmd) {
        if (requireCommand(cmd, ctx)) {return;}

        Integer gameID = cmd.getGameID();
        String token = cmd.getAuthToken();

        try {
            Auth auth = requireAuth(token, ctx);
            if (auth == null) {return;}

            GameConnections gc = Games.get(gameID);
            if (gc == null) {
                sendError(ctx, "error: game not found");
                return;
            }

            gc.removeSession(ctx);

            Game model = dao.getGame(gameID);
            if (model != null) {
                String username = auth.username();

                if (username.equals(model.whiteUsername())) {
                    dao.updateGame(new Game(model.gameID(), null,
                            model.blackUsername(), model.gameName(), model.game()));

                } else if (username.equals(model.blackUsername())) {
                    dao.updateGame(new Game(model.gameID(), model.whiteUsername(),
                            null, model.gameName(), model.game()));
                }

                gc.broadcastNotification(
                        new NotificationMessage(auth.username() + " left the game"));
            }

        } catch (DataAccessException ex) {
            sendError(ctx, "error: server data error");
        }
    }

    private void handleResign(WsContext ctx, UserGameCommand cmd) {
        if (requireCommand(cmd, ctx)) {return;}

        Integer gameID = cmd.getGameID();
        String token = cmd.getAuthToken();

        try {
            Auth auth = requireAuth(token, ctx);
            if (auth == null) {return;}

            Game model = requireGame(gameID, ctx);
            if (model == null) {return;}

            String username = auth.username();
            if (!username.equals(model.whiteUsername()) &&
                    !username.equals(model.blackUsername())) {
                sendError(ctx, "error: you are not a player in this game");
                return;
            }

            if (model.game() == null) {
                sendError(ctx, "error: game already over");
                return;
            }

            dao.updateGame(new Game(model.gameID(), model.whiteUsername(),
                    model.blackUsername(), model.gameName(), null));

            GameConnections gc = Games.get(gameID);
            if (gc != null) {
                gc.broadcastNotification(new NotificationMessage(username + " resigned"));
            }

        } catch (DataAccessException ex) {
            sendError(ctx, "error: server data error");
        }
    }

    private void handleMakeMove(WsContext ctx, MakeMoveCommand cmd) {
        if (requireCommand(cmd, ctx)) {return;}

        Integer gameID = cmd.getGameID();
        String token = cmd.getAuthToken();

        try {
            Auth auth = requireAuth(token, ctx);
            if (auth == null) {return;}

            Game model = requireGame(gameID, ctx);
            if (model == null) {return;}

            ChessGame.TeamColor team = getPlayerTeam(model, auth.username(), ctx);
            if (team == null) {return;}

            ChessGame chessGame = model.game();
            if (chessGame == null) {
                sendError(ctx, "error: game internal state missing");
                return;
            }

            var dto = cmd.move;
            if (dto == null || dto.start == null || dto.end == null) {
                sendError(ctx, "error: missing move data");
                return;
            }

            if (chessGame.getTeamTurn() != team) {
                sendError(ctx, "error: not your turn");
                return;
            }

            ChessPiece.PieceType promo = null;
            if (dto.promotion != null) {
                try {
                    promo = ChessPiece.PieceType.valueOf(dto.promotion.toUpperCase());
                } catch (IllegalArgumentException ex) {
                    sendError(ctx, "error: illegal move");
                    return;
                }
            }

            ChessMove move = new ChessMove(dto.start, dto.end, promo);

            try {
                chessGame.makeMove(move);
            } catch (InvalidMoveException ex) {
                sendError(ctx, "error: invalid move");
                return;
            }

            dao.updateGame(new Game(model.gameID(), model.whiteUsername(),
                    model.blackUsername(), model.gameName(), chessGame));

            Game fresh = dao.getGame(gameID);

            GameConnections gc = Games.get(gameID);
            if (gc != null) {
                gc.broadcastJson(new LoadGameMessage(fresh));

                gc.broadcastNotificationExcept(
                        new NotificationMessage(auth.username() + " moved " + dto.toReadable()),
                        ctx);
            }

        } catch (DataAccessException ex) {
            sendError(ctx, "error: server data error");
        }
    }

    private void sendError(WsContext ctx, String msg) {
        sendJson(ctx, new ErrorMessage(msg));
    }

    private void sendJson(WsContext ctx, ServerMessage msg) {
        ctx.send(GSON.toJson(msg));
    }
}
