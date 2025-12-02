package websocket;

import chess.ChessGame;
import model.Game;

public class GameDTO {
    public int gameID;
    public String whiteUsername;
    public String blackUsername;
    public String gameName;
    public ChessGame game;
    public String status;

    public static GameDTO fromModel(Game model) {
        GameDTO dto = new GameDTO();
        dto.gameID = model.gameID();
        dto.whiteUsername = model.whiteUsername();
        dto.blackUsername = model.blackUsername();
        dto.gameName = model.gameName();
        dto.game = model.game();
        dto.status = "ONGOING";
        return dto;
    }
}
