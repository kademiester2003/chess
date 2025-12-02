package chess;

import com.google.gson.Gson;

public class ChessGameAdapter {

    private static final Gson gson = new Gson();

    public static ChessGame fromJson(String json) {
        return gson.fromJson(json, ChessGame.class);
    }

    public static String toJson(ChessGame game) {
        return gson.toJson(game);
    }

    public static boolean tryApplyMove(ChessGame game, ChessMove move) {
        try {
            game.makeMove(move);
            return true;
        } catch (InvalidMoveException ex) {
            return false;
        }
    }
}
