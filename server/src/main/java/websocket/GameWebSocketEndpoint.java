package websocket;

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
    private static final Gson gson = new GsonBuilder().create();
    private static final ConcurrentHashMap<Integer, GameConnections> games = new ConcurrentHashMap<>();
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
            case "CONNECT" -> handleConnect(ctx, gson.fromJson(root, UserGameCommand.class));
            case "LEAVE" -> handleLeave(ctx, gson.fromJson(root, UserGameCommand.class));
            case "RESIGN" -> handleResign(ctx, gson.fromJson(root, UserGameCommand.class));
            case "MAKE_MOVE" -> handleMakeMove(ctx, gson.fromJson(root, MakeMoveCommand.class));
            default -> sendError(ctx, "error: unknown command");
        }
    }

    public void onClose(WsCloseContext ctx) {}

    public void onError(WsErrorContext ctx, Throwable thr) {}

    private void handleConnect(WsContext ctx, UserGameCommand cmd) {
        if (cmd == null) {
            sendError(ctx, "error: invalid command");
            return;
        }
        Integer gameID = cmd.getGameID();
        String token = cmd.getAuthToken();

        try {
            Auth auth = dao.getAuth(token);
            if (auth == null) {
                sendError(ctx, "error: invalid auth token");
                return;
            }

            Game game = dao.getGame(gameID);
            if (game == null) {
                sendError(ctx, "error: game not found");
                return;
            }

            GameConnections gc = games.compute(gameID, (id, existing) -> existing != null ? existing : new GameConnections(gameID));
            gc.addSession(ctx, auth.username());

            LoadGameMessage load = new LoadGameMessage(GameDTO.fromModel(game));
            sendJson(ctx, load);

            String side = gc.getSideForUsername(auth.username(), game);
            gc.broadcastNotificationExcept(new NotificationMessage(auth.username() + "connected as" + side), ctx);

        } catch (DataAccessException ex) {
            sendError(ctx, "error: server data error");
        }
    }

    private void handleLeave(WsContext ctx, UserGameCommand cmd) {
        if (cmd == null) {
            sendError(ctx, "error: invalid command");
            return;
        }
        Integer gameID = cmd.getGameID();
        String token = cmd.getAuthToken();

        try {
            Auth auth = dao.getAuth(token);
            if (auth == null) {
                sendError(ctx, "error: invalid auth token");
                return;
            }

            GameConnections gc = games.get(gameID);
            if (gc != null) {
                gc.removeSession(ctx);
                Game model = dao.getGame(gameID);
                if (model != null) {
                    String username = auth.username();
                    if (username.equals(model.whiteUsername())) {
                        Game updatedModel = new Game(model.gameID(), null, model.blackUsername(), model.gameName(), model.game());
                        dao.updateGame(updatedModel);
                    } else if (username.equals(model.blackUsername())) {
                        Game updatedModel = new Game(model.gameID(), model.whiteUsername(), null, model.gameName(), model.game());
                        dao.updateGame(updatedModel);
                    }
                    String msg = auth.username() + " left the game";
                    gc.broadcastNotification(new NotificationMessage(msg));
                }
            } else {
                sendError(ctx, "error: game not found");
                return;
            }
        } catch (DataAccessException ex) {
            sendError(ctx, "error: server data error");
        }
    }

    private void handleResign(WsContext ctx, UserGameCommand cmd) {
        if (cmd == null) {
            sendError(ctx, "error: invalid command");
            return;
        }
        Integer gameID = cmd.getGameID();
        String token = cmd.getAuthToken();

        try {
            Auth auth = dao.getAuth(token);
            if (auth == null) {
                sendError(ctx, "error: invalid auth token");
                return;
            }

            Game model = dao.getGame(gameID);
            if (model == null) {
                sendError(ctx, "error: game not found");
                return;
            }

            String username = auth.username();
            if (!username.equals(model.whiteUsername()) && !username.equals(model.blackUsername())) {
                sendError(ctx, "error: you are not a player in this game");
                return;
            }

            if (model.game() == null) {
                sendError(ctx, "error: game already over");
                return;
            }

            Game updatedModel = new Game(model.gameID(), model.whiteUsername(), model.blackUsername(), model.gameName(), null);
            dao.updateGame(updatedModel);

            GameConnections gc = games.get(gameID);
            if (gc != null) {
                String msg = auth.username() + " resigned";
                gc.broadcastNotification(new NotificationMessage(msg));
                //LoadGameMessage load = new LoadGameMessage(GameDTO.fromModel(model));
                //gc.broadcastJson(load);
            }
        } catch (DataAccessException ex) {
            sendError(ctx, "error: server data error");
        }
    }

    private void handleMakeMove(WsContext ctx, MakeMoveCommand cmd) {
        if (cmd == null) {
            sendError(ctx, "error: invalid command");
            return;
        }
        Integer gameID = cmd.getGameID();
        String token = cmd.getAuthToken();

        try {
            Auth auth = dao.getAuth(token);
            if (auth == null) {
                sendError(ctx, "error: invalid auth token");
                return;
            }

            Game model = dao.getGame(gameID);
            if (model == null) {
                sendError(ctx, "error: game not found");
                return;
            }

            String username = auth.username();
            ChessGame.TeamColor playerTeam = null;
            if (username.equals(model.whiteUsername())) {
                playerTeam = ChessGame.TeamColor.WHITE;
            } else if (username.equals(model.blackUsername())) {
                playerTeam = ChessGame.TeamColor.BLACK;
            } else {
                sendError(ctx, "error: you are not a player in this game");
                return;
            }

            ChessGame chessGame = model.game();
            if (chessGame == null) {
                sendError(ctx, "error: game internal state missing");
                return;
            }

            if (chessGame.getTeamTurn() != playerTeam) {
                sendError(ctx, "error: not your turn");
                return;
            }

            var dto = cmd.move;
            if (dto == null || dto.start == null || dto.end == null) {
                sendError(ctx, "error: missing move data");
                return;
            }

            ChessPosition startPos = dto.start;
            ChessPosition endPos = dto.end;

            ChessPiece.PieceType promo = null;
            if (dto.promotion != null) {
                promo = ChessPiece.PieceType.valueOf(dto.promotion.toUpperCase());
            }

            ChessMove matchingMove = new ChessMove(startPos, endPos, promo);
            try {
                chessGame.makeMove(matchingMove);
            } catch (InvalidMoveException e) {
                sendError(ctx, "error: illegal move");
                return;
            }

            Game updated = new Game(model.gameID(), model.whiteUsername(), model.blackUsername(), model.gameName(), chessGame);
            dao.updateGame(updated);

            GameConnections gc = games.get(gameID);
            if (gc != null) {
                LoadGameMessage load = new LoadGameMessage(GameDTO.fromModel(dao.getGame(gameID)));
                gc.broadcastJsonExcept(load, ctx);

                String moveText = auth.username() + " moved " + dto.toReadable();
                gc.broadcastNotificationExcept(new NotificationMessage(moveText), ctx);
            }

        } catch (DataAccessException ex) {
            sendError(ctx, "error: server data error");
        }
    }

    private ChessPosition parseAlg(String str) {
        if (str == null || !str.matches("^[a-h][1-8]$")) {
            return null;
        }
        int col = (str.charAt(0) - 'a') + 1;
        int row = Character.getNumericValue(str.charAt(1)) - 1;
        return new ChessPosition(row, col);
    }

    private void sendError(WsContext ctx, String msg) {
        sendJson(ctx, new ErrorMessage(msg));
    }

    private void sendJson(WsContext ctx, ServerMessage msg) {
        ctx.send(gson.toJson(msg));
    }
}
