package client;

import chess.*;
import ui.EscapeSequences;

public class BoardDrawer {

    public static void drawBoard(ChessGame game, ChessGame.TeamColor perspective) {
        System.out.print(EscapeSequences.ERASE_SCREEN);

        ChessBoard board = game.getBoard();

        // Black perspective → flip ranks/files
        int startRank = (perspective == ChessGame.TeamColor.WHITE) ? 8 : 1;
        int endRank   = (perspective == ChessGame.TeamColor.WHITE) ? 1 : 8;
        int rankStep  = (perspective == ChessGame.TeamColor.WHITE) ? -1 : 1;

        int startFile = (perspective == ChessGame.TeamColor.WHITE) ? 1 : 8;
        int endFile   = (perspective == ChessGame.TeamColor.WHITE) ? 8 : 1;
        int fileStep  = (perspective == ChessGame.TeamColor.WHITE) ? 1 : -1;

        System.out.println();

        for (int r = startRank; r != endRank + rankStep; r += rankStep) {
            // Row number label
            System.out.print(" " + r + " ");

            for (int f = startFile; f != endFile + fileStep; f += fileStep) {
                String bg = ((r + f) % 2 == 0) ?
                        EscapeSequences.SET_BG_COLOR_LIGHT_GREY :
                        EscapeSequences.SET_BG_COLOR_DARK_GREY;

                ChessPosition pos = new ChessPosition(r, f);
                ChessPiece piece = board.getPiece(pos);

                System.out.print(bg);

                if (piece == null) {
                    System.out.print(EscapeSequences.EMPTY);
                } else {
                    System.out.print(convertPiece(piece));
                }

                System.out.print(EscapeSequences.RESET_BG_COLOR);
            }
            System.out.println();
        }

        // File labels (a–h)
        System.out.print("\n    ");
        for (char c = 'a'; c <= 'h'; c++) {
            System.out.print(" " + c + "  ");
        }
        System.out.println("\n");
    }

    private static String convertPiece(ChessPiece piece) {
        return switch (piece.getTeamColor()) {
            case WHITE -> switch (piece.getPieceType()) {
                case KING -> EscapeSequences.WHITE_KING;
                case QUEEN -> EscapeSequences.WHITE_QUEEN;
                case ROOK -> EscapeSequences.WHITE_ROOK;
                case KNIGHT -> EscapeSequences.WHITE_KNIGHT;
                case BISHOP -> EscapeSequences.WHITE_BISHOP;
                case PAWN -> EscapeSequences.WHITE_PAWN;
            };
            case BLACK -> switch (piece.getPieceType()) {
                case KING -> EscapeSequences.BLACK_KING;
                case QUEEN -> EscapeSequences.BLACK_QUEEN;
                case ROOK -> EscapeSequences.BLACK_ROOK;
                case KNIGHT -> EscapeSequences.BLACK_KNIGHT;
                case BISHOP -> EscapeSequences.BLACK_BISHOP;
                case PAWN -> EscapeSequences.BLACK_PAWN;
            };
        };
    }
}
