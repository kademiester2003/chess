package websocket;

import chess.*;
import com.google.gson.*;
import com.google.gson.GsonBuilder;
import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import dataaccess.MemoryDataAccess;
import dataaccess.MySQLDataAccess;

import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.websocket.*;
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
        switch (ctStr) {
            case "CONNECT" -> handleConnect(session, gson.fromJson(root, UserGameCommand.class));
            case "LEAVE" -> handleLeave(session, gson.fromJson(root, UserGameCommand.class));
            case "RESIGN" -> handleResign(session, gson.fromJson(root, UserGameCommand.class));
            case "MAKE_MOVE" -> handleMakeMove(session, gson.fromJson(root, MakeMoveCommand.class));
            default -> sendError(session, "error: unknown command");
        }
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

    private void handleLeave(Session session, UserGameCommand cmd) {
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
            }

            GameConnections gc = games.get(gameID);
            if (gc != null) {
                gc.removeSession(session);
                Game model = dao.getGame(gameID);
                if (model != null) {
                    boolean updated = false;
                    String username = auth.username();
                    if (username.equals(model.whiteUsername())) {
                        Game updatedModel = new Game(model.gameID(), null, model.blackUsername(), model.gameName(), model.game());
                        dao.updateGame(updatedModel);
                        updated = true;
                    } else if (username.equals(model.blackUsername())) {
                        Game updatedModel = new Game(model.gameID(), model.whiteUsername(), null, model.gameName(), model.game());
                        dao.updateGame(updatedModel);
                        updated = true;
                    }
                    String msg = auth.username() + " left the game";
                    gc.broadcastNotification(new NotificationMessage(msg));
                }
            } else {
                sendError(session, "error: game not found");
            }
        } catch (DataAccessException ex) {
            sendError(session, "error: server data error");
        }
    }

    private void handleResign(Session session, UserGameCommand cmd) {
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

            Game model = dao.getGame(gameID);
            if (model == null) {
                sendError(session, "error: game not found");
                return;
            }

            GameConnections gc = games.get(gameID);
            if (gc != null) {
                String msg = auth.username() + " resigned";
                gc.broadcastNotification(new NotificationMessage(msg));
                LoadGameMessage load = new LoadGameMessage(GameDTO.fromModel(model));
                gc.broadcastJson(load);
            }
        } catch (DataAccessException ex) {
            sendError(session, "error: server data error");
        }
    }

    private void handleMakeMove(Session session, MakeMoveCommand cmd) {
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

            Game model = dao.getGame(gameID);
            if (model == null) {
                sendError(session, "error: game not found");
                return;
            }

            String username = auth.username();
            ChessGame.TeamColor playerTeam = null;
            if (username.equals(model.whiteUsername())) {
                playerTeam = ChessGame.TeamColor.WHITE;
            } else if (username.equals(model.blackUsername())) {
                playerTeam = ChessGame.TeamColor.BLACK;
            } else {
                sendError(session, "error: you are not a player in this game");
                return;
            }

            ChessGame chessGame = model.game();
            if (chessGame == null) {
                sendError(session, "error: game internal state missing");
                return;
            }

            if (chessGame.getTeamTurn() != playerTeam) {
                sendError(session, "error: not your turn");
                return;
            }

            MakeMoveCommand.ChessMoveDto dto = cmd.getMove();
            if (dto == null || dto.start == null || dto.end == null) {
                sendError(session, "error: missing move data");
                return;
            }

            ChessPosition startPos = new ChessPosition(dto.start.row + 1, dto.start.col + 1);
            ChessPosition endPos = new ChessPosition(dto.end.row + 1, dto.end.col + 1);

            Collection<ChessMove> validMoves = chessGame.validMoves(startPos);
            ChessMove matchingMove = null;
            if (validMoves != null) {
                for (ChessMove m : validMoves) {
                    if (m.getEndPosition().equals(endPos)) {
                        if (dto.promotion == null) {
                            if (m.getPromotionPiece() == null) {
                                matchingMove = m;
                                break;
                            }
                        } else {
                            if (m.getPromotionPiece() != null &&
                                    m.getPromotionPiece().name().equalsIgnoreCase(dto.promotion)) {
                                matchingMove = m;
                                break;
                            }
                        }
                    }
                }
            }

            if (matchingMove == null) {
                sendError(session, "error: illegal move");
                return;
            }

            try {
                chessGame.makeMove(matchingMove);
            } catch (InvalidMoveException e) {
                sendError(session, "error: illegal move");
                return;
            }

            dao.updateGame(model);

            GameConnections gc = games.get(gameID);
            if (gc != null) {
                LoadGameMessage load = new LoadGameMessage(GameDTO.fromModel(dao.getGame(gameID)));
                gc.broadcastJson(load);

                String moveText = auth.username() + " moved " + dto.toReadable();
                gc.broadcastNotification(new NotificationMessage(moveText));
            }

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

    private void sendJson(Session session, ServerMessage msg) {
        try {
            String json = gson.toJson(msg);
            session.getBasicRemote().sendText(json);
        } catch (IOException ex) {}
    }
}
