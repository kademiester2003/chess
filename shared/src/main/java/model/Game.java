package model;

import chess.ChessGame;

public record Game(int gameID, String whiteUsername, String blackUsername, String gameName, ChessGame game) {
    public String currentUser() {
        return switch (game.getTeamTurn()) {
            case WHITE -> whiteUsername;
            case BLACK -> blackUsername;
        };
    }
}
