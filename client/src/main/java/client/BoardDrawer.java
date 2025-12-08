package client;

import chess.*;
import ui.EscapeSequences;

import java.util.Collection;

public class BoardDrawer {

    public static void drawInitialBoard(ChessGame.TeamColor perspective) {
        drawBoard(new ChessGame(), perspective);
    }

    public static void drawBoard(ChessGame game, ChessGame.TeamColor perspective) {
        drawBoardWithHighlights(game, perspective, null, null);
    }

    public static void drawBoardWithHighlights(
            ChessGame game,
            ChessGame.TeamColor perspective,
            ChessPosition selected,
            Collection<ChessMove> legalMoves
    ) {
        System.out.print(EscapeSequences.ERASE_SCREEN + EscapeSequences.moveCursorToLocation(1, 1));

        ChessBoard board = game.getBoard();

        int startRank = (perspective == ChessGame.TeamColor.WHITE) ? 8 : 1;
        int endRank = (perspective == ChessGame.TeamColor.WHITE) ? 1 : 8;
        int rankStep = (perspective == ChessGame.TeamColor.WHITE) ? -1 : 1;

        int startFile = (perspective == ChessGame.TeamColor.WHITE) ? 1 : 8;
        int endFile = (perspective == ChessGame.TeamColor.WHITE) ? 8 : 1;
        int fileStep = (perspective == ChessGame.TeamColor.WHITE) ? 1 : -1;

        System.out.println();

        for (int r = startRank; r != endRank + rankStep; r += rankStep) {
            System.out.print(" " + r + " ");

            for (int f = startFile; f != endFile + fileStep; f += fileStep) {

                ChessPosition pos = new ChessPosition(r, f);
                ChessPiece piece = board.getPiece(pos);

                String bg;

                if (selected != null && selected.equals(pos)) {
                    bg = EscapeSequences.SET_BG_COLOR_BLUE;
                }
                else if (legalMoves != null && legalMoves.stream().anyMatch(m -> m.getEndPosition().equals(pos))) {
                    bg = EscapeSequences.SET_BG_COLOR_GREEN;
                }
                else {
                    bg = ((r + f) % 2 == 0)
                            ? EscapeSequences.SET_BG_COLOR_DARK_GREY
                            : EscapeSequences.SET_BG_COLOR_LIGHT_GREY;
                }

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

        System.out.print("   ");
        if (perspective == ChessGame.TeamColor.WHITE) {
            for (char c = 'a'; c <= 'h'; c++) {
                System.out.print(" " + c + " ");
            }
        } else {
            for (char c = 'h'; c >= 'a'; c--) {
                System.out.print(" " + c + " ");
            }
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
