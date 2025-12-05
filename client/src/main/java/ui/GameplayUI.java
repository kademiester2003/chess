package ui;

import chess.ChessPiece;
import chess.ChessPosition;
import client.ChessWS;
import websocket.commands.MakeMoveCommand;

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
            String line = scanner.nextLine().trim();
            if (line.equalsIgnoreCase("help")) {
                printHelp();
            }

            else if (line.equalsIgnoreCase("redraw")) {
                System.out.println("Requesting redraw (send a CONNECT again to force LOAD_GAME).");
                ws.sendConnect(authToken, gameID);
            }

            else if (line.equalsIgnoreCase("leave")) {
                ws.sendLeave(authToken, gameID);
            }

            else if (line.equalsIgnoreCase("resign")) {
                System.out.print("Confirm resign? (y/N): ");
                String yn = scanner.nextLine().trim();
                if (yn.equalsIgnoreCase("y")) {
                    ws.sendResign(authToken, gameID);
                } else {
                    System.out.println("Resign cancelled.");
                }
            }

            else if (line.toLowerCase().startsWith("move")) {
                String[] parts = line.split("\\s+");
                if (parts.length < 3) {
                    System.out.println("Usage: move <from> <to> [promotion], example: move e2 e4");
                    return;
                }

                String start = parts[1].toLowerCase();
                String end = parts[2].toLowerCase();

                if (!isValidAlg(start) || !isValidAlg(end)) {
                    System.out.println("Invalid input.");
                    return;
                }

                ChessPiece.PieceType promotion = null;
                if (parts.length >= 4) {
                    try {
                        promotion = ChessPiece.PieceType.valueOf(parts[3].toUpperCase());
                    } catch (IllegalArgumentException e) {
                        System.out.println("Invalid promotion.");
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

            else if (line.equalsIgnoreCase("quit") || line.equalsIgnoreCase("exit")) {
                try {
                    ws.close();
                } catch (Exception ignored) {}
                break;
            }

            else {
                System.out.println("Unknown command. Type 'help' to list commands.");
            }
        }
    }

    private boolean isValidAlg(String str) {
        return str.matches("[a-h][1-8]$");
    }

    private ChessPosition parseAlg(String str) {
        int col = (str.charAt(0) - 'a') + 1;
        int row = Character.getNumericValue(str.charAt(1));
        return new ChessPosition(row, col);
    }

    private void printHelp() {
        System.out.println("Commands:");
        System.out.println("  help           - show this help");
        System.out.println("  redraw         - request redraw of the board");
        System.out.println("  leave          - leave the game (return to post-login UI)");
        System.out.println("  resign         - resign the game");
        System.out.println("  move <from> <to> [promotion]  - make a move, e.g. move e2 e4");
        System.out.println("  quit / exit    - close the client");
    }
}
