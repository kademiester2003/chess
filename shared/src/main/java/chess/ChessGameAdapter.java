package chess;

import com.google.gson.*;

import java.lang.reflect.Type;

public class ChessGameAdapter implements JsonSerializer<ChessGame>, JsonDeserializer<ChessGame> {

    @Override
    public JsonElement serialize(ChessGame src, Type type, JsonSerializationContext context) {
        return context.serialize(src.getBoard());
    }

    @Override
    public ChessGame deserialize(JsonElement json, Type type, JsonDeserializationContext context)
            throws JsonParseException {

        ChessBoard board = context.deserialize(json, ChessBoard.class);
        ChessGame game = new ChessGame();
        game.setBoard(board);
        return game;
    }

    // WebSocket helper used in GameWebSocketEndpoint
    public static boolean tryApplyMove(ChessGame game, ChessMove move) {
        try {
            game.makeMove(move);
            return true;
        } catch (InvalidMoveException ex) {
            return false;
        }
    }
}
