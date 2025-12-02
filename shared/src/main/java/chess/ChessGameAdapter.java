package chess;

import com.google.gson.*;
import java.lang.reflect.Type;

public class ChessGameAdapter implements JsonSerializer<ChessGame>, JsonDeserializer<ChessGame> {

    @Override
    public JsonElement serialize(ChessGame src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject obj = new JsonObject();
        obj.addProperty("teamTurn", src.getTeamTurn().name());
        obj.add("board", context.serialize(src.getBoard()));
        return obj;
    }

    @Override
    public ChessGame deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject obj = json.getAsJsonObject();
        ChessGame game = new ChessGame();
        ChessGame.TeamColor teamTurn = ChessGame.TeamColor.valueOf(obj.get("teamTurn").getAsString());
        game.setTeamTurn(teamTurn);
        ChessBoard board = context.deserialize(obj.get("board"), ChessBoard.class);
        game.setBoard(board);
        return game;
    }
}
