package ui;

import chess.*;
import client.ChessWS;
import client.BoardDrawer;
import websocket.commands.UserGameCommand;

import java.util.Collection;
import java.util.Scanner;

public class GameplayUI {
    private final ChessWS ws;
    private final Scanner scanner = new Scanner(System.in);
    private final String authToken;
    private final int gameID;

    public GameplayUI(String serverWsUrl, String authToken, int gameID, String localUsername) throws Exception {
        this.ws = new ChessWS(serverWsUrl);
        this.ws.setLocalUsername(localUsername);
        this.authToken = authToken;
        this.gameID = gameID;

        ws.connect();
        ws.setLocalUserToken(authToken);
    }

    public void run() {
        ws.sendConnect(authToken, gameID);
        System.out.println("Entered gameplay UI. Type 'help' for commands.");

        while (ws.isConnected()) {
            System.out.print("> ");
            String line = scanner.nextLine();
            if (line == null) {break;}

            line = line.trim().toLowerCase();
            if (line.isEmpty()) {continue;}

            String cmd = line.split("\\s+")[0];

            switch (cmd) {
                case "help" -> printHelp();

                case "redraw" -> {
                    if (ws.getCurrentGame() == null) {
                        System.out.println("Game not loaded yet. Requesting state...");
                        ws.sendConnect(authToken, gameID);
                    } else {
                        BoardDrawer.drawBoard(ws.getCurrentGame(), ws.getPerspective());
                    }
                }

                case "leave" -> {
                    ws.sendLeave(authToken, gameID);
                    System.out.println("Sent LEAVE request to server.");
                    ws.close();
                }

                case "resign" -> {
                    System.out.print("Confirm resign? (y/N): ");
                    if (scanner.nextLine().trim().equalsIgnoreCase("y")) {
                        ws.sendResign(authToken, gameID);
                        System.out.println("Sent RESIGN request to server.");
                    } else {
                        System.out.println("Resign cancelled.");
                    }
                }

                case "move" -> handleMove(line);

                case "highlight" -> handleHighlight(line);

                default -> System.out.println("Unknown command. Type 'help' to list commands.");
            }
        }
    }

    private void handleMove(String line) {
        String[] parts = line.split("\\s+");

        if (parts.length < 3) {
            System.out.println("Usage: move <from> <to> [promotion]");
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
            } catch (Exception e) {
                System.out.println("Invalid promotion piece.");
                return;
            }
        }

        ChessPosition startPos = parseAlg(start);
        ChessPosition endPos = parseAlg(end);

        UserGameCommand.Move move = new UserGameCommand.Move();
        move.start = startPos;
        move.end = endPos;
        move.promotion = (promotion != null) ? promotion.name() : null;

        ws.sendMakeMove(authToken, gameID, move);
        System.out.println("Move sent. Waiting for server to update board...");
    }

    private void handleHighlight(String line) {
        String[] parts = line.split("\\s+");

        if (parts.length < 2) {
            System.out.println("Usage: highlight <square>");
            return;
        }

        String sq = parts[1];
        if (!isValidAlg(sq)) {
            System.out.println("Invalid notation.");
            return;
        }

        ChessGame game = ws.getCurrentGame();
        if (game == null) {
            System.out.println("Game not loaded.");
            return;
        }

        ChessPosition pos = parseAlg(sq);
        ChessPiece piece = game.getBoard().getPiece(pos);

        if (piece == null) {
            System.out.println("No piece at " + sq);
            return;
        }

        Collection<ChessMove> legalMoves = piece.pieceMoves(game.getBoard(), pos);

        BoardDrawer.drawBoardWithHighlights(game, ws.getPerspective(), pos, legalMoves);
    }

    private boolean isValidAlg(String s) {
        return s.matches("[a-h][1-8]");
    }

    private ChessPosition parseAlg(String s) {
        int col = s.charAt(0) - 'a' + 1;
        int row = s.charAt(1) - '0';
        return new ChessPosition(row, col);
    }

    private void printHelp() {
        System.out.println("Gameplay Commands:");
        System.out.println("  help                     - show help");
        System.out.println("  redraw                   - redraw the board");
        System.out.println("  leave                    - leave the game");
        System.out.println("  resign                   - resign from the game");
        System.out.println("  move <from> <to> [promo] - make a move");
        System.out.println("  highlight <square>       - show legal moves");
    }
}
