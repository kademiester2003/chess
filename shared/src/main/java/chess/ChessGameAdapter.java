package chess;

import com.google.gson.*;
import java.lang.reflect.Type;

public class ChessGameAdapter implements JsonSerializer<ChessGame>, JsonDeserializer<ChessGame> {

    @Override
    public JsonElement serialize(ChessGame src, Type typeOfSrc, JsonSerializationContext ctx) {
        JsonObject obj = new JsonObject();

        obj.add("board", ctx.serialize(src.getBoard()));
        obj.addProperty("teamTurn", src.getTeamTurn().name());

        return obj;
    }

    @Override
    public ChessGame deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext ctx)
            throws JsonParseException {

        JsonObject obj = json.getAsJsonObject();

        ChessBoard board = ctx.deserialize(obj.get("board"), ChessBoard.class);

        ChessGame.TeamColor turn =
                ChessGame.TeamColor.valueOf(obj.get("teamTurn").getAsString());

        ChessGame game = new ChessGame();
        game.setBoard(board);
        game.setTeamTurn(turn);

        return game;
    }
}
