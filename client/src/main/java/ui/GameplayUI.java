package ui;

import chess.*;
import client.ChessWS;
import client.BoardDrawer;
import websocket.commands.MakeMoveCommand;

import java.util.Collection;
import java.util.Scanner;

public class GameplayUI {
    private final ChessWS ws;
    private final Scanner scanner = new Scanner(System.in);
    private final String authToken;
    private final int gameID;

    public GameplayUI(String serverWsUrl, String authToken, int gameID) throws Exception {
        this.ws = new ChessWS(serverWsUrl);
        this.authToken = authToken;
        this.gameID = gameID;
        ws.connect();
    }

    public void run() {
        ws.sendConnect(authToken, gameID);
        System.out.println("Entered gameplay UI. Type 'help' for commands.");

        while (ws.isConnected()) {
            System.out.print("> ");
            String line = scanner.nextLine().trim().toLowerCase();

            switch (line.split("\\s+")[0]) {

                case "help" -> printHelp();

                case "redraw" -> {
                    if (ws.getCurrentGame() == null) {
                        System.out.println("Game not loaded yet. Requesting state...");
                        ws.sendConnect(authToken, gameID);
                    } else {
                        BoardDrawer.drawBoard(ws.getCurrentGame(), ws.getPerspective());
                    }
                }

                case "leave" -> ws.sendLeave(authToken, gameID);

                case "resign" -> {
                    System.out.print("Confirm resign? (y/N): ");
                    if (scanner.nextLine().trim().equalsIgnoreCase("y")) {
                        ws.sendResign(authToken, gameID);
                    } else {
                        System.out.println("Resign cancelled.");
                    }
                }

                case "move" -> handleMove(line);

                case "highlight" -> handleHighlight(line);

                case "quit", "exit" -> {
                    try { ws.close(); } catch (Exception ignored) {}
                    return;
                }

                default -> System.out.println("Unknown command. Type 'help' to list commands.");
            }
        }
    }

    private void handleMove(String line) {
        String[] parts = line.split("\\s+");

        if (parts.length < 3) {
            System.out.println("Usage: move <from> <to> [promotion], example: move e2 e4");
            return;
        }

        String start = parts[1];
        String end = parts[2];

        if (!isValidAlg(start) || !isValidAlg(end)) {
            System.out.println("Invalid coordinate. Use algebraic notation like e2.");
            return;
        }

        ChessPiece.PieceType promotion = null;
        if (parts.length >= 4) {
            try {
                promotion = ChessPiece.PieceType.valueOf(parts[3].toUpperCase());
            } catch (IllegalArgumentException e) {
                System.out.println("Invalid promotion piece. Use: QUEEN ROOK BISHOP KNIGHT");
                return;
            }
        }

        ChessPosition startPos = parseAlg(start);
        ChessPosition endPos = parseAlg(end);

        MakeMoveCommand.Move move = new MakeMoveCommand.Move();
        move.start = startPos;
        move.end = endPos;
        move.promotion = (promotion != null) ? promotion.name() : null;

        ws.sendMakeMove(authToken, gameID, move);
    }

    /** Client-only highlight feature **/
    private void handleHighlight(String line) {
        String[] parts = line.split("\\s+");

        if (parts.length < 2) {
            System.out.println("Usage: highlight <square>, e.g. highlight e2");
            return;
        }

        String sq = parts[1];

        if (!isValidAlg(sq)) {
            System.out.println("Invalid square.");
            return;
        }

        ChessPosition pos = parseAlg(sq);

        ChessGame game = ws.getCurrentGame();
        if (game == null) {
            System.out.println("Game not yet loaded.");
            return;
        }

        ChessPiece piece = game.getBoard().getPiece(pos);
        if (piece == null) {
            System.out.println("No piece on " + sq);
            return;
        }

        Collection<ChessMove> legalMoves = piece.pieceMoves(game.getBoard(), pos);

        System.out.println("Highlighting moves for piece on " + sq);
        BoardDrawer.drawBoardWithHighlights(game, ws.getPerspective(), pos, legalMoves);
    }

    private boolean isValidAlg(String str) {
        return str.matches("[a-h][1-8]");
    }

    private ChessPosition parseAlg(String str) {
        int col = str.charAt(0) - 'a' + 1;
        int row = str.charAt(1) - '0';
        return new ChessPosition(row, col);
    }

    private void printHelp() {
        System.out.println("Gameplay Commands:");
        System.out.println("  help                     - show this help");
        System.out.println("  redraw                   - redraw the chess board");
        System.out.println("  leave                    - leave the game");
        System.out.println("  resign                   - resign the game");
        System.out.println("  move <from> <to> [promo] - make a move (e.g., move e2 e4)");
        System.out.println("  highlight <square>       - highlight legal moves of a piece (local only)");
        System.out.println("  quit / exit              - close the client");
    }
}
