package ui;

import websocket.ChessWS;
import websocket.commands.MakeMoveCommand;

import java.util.Scanner;

/**
 * Simple console UI to demonstrate WebSocket commands.
 */
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
            } else if (line.equalsIgnoreCase("redraw")) {
                System.out.println("Requesting redraw (send a CONNECT again to force LOAD_GAME).");
                ws.sendConnect(authToken, gameID);
            } else if (line.equalsIgnoreCase("leave")) {
                ws.sendLeave(authToken, gameID);
            } else if (line.equalsIgnoreCase("resign")) {
                System.out.print("Confirm resign? (y/N): ");
                String yn = scanner.nextLine().trim();
                if (yn.equalsIgnoreCase("y")) {
                    ws.sendResign(authToken, gameID);
                } else {
                    System.out.println("Resign cancelled.");
                }
            } else if (line.toLowerCase().startsWith("move")) {
                // expected: move e2 e4  (simple algebraic split)
                String[] parts = line.split("\\s+");
                if (parts.length >= 3) {
                    MakeMoveCommand.ChessMoveDto move = new MakeMoveCommand.ChessMoveDto();
                    move.start = parseSquare(parts[1]);
                    move.end = parseSquare(parts[2]);
                    if (parts.length == 4) move.promotion = parts[3];
                    ws.sendMakeMove(authToken, gameID, move);
                } else {
                    System.out.println("Usage: move <from> <to> [promotion], example: move e2 e4");
                }
            } else if (line.equalsIgnoreCase("quit") || line.equalsIgnoreCase("exit")) {
                try { ws.close(); } catch (Exception ignored) {}
                break;
            } else {
                System.out.println("Unknown command. Type 'help' to list commands.");
            }
        }
    }

    private MakeMoveCommand.ChessMoveDto.Square parseSquare(String alg) {
        // very simple parser: file letter + rank number, e.g. e2
        MakeMoveCommand.ChessMoveDto.Square s = new MakeMoveCommand.ChessMoveDto.Square();
        if (alg.length() >= 2) {
            char file = alg.charAt(0);
            char rank = alg.charAt(1);
            s.col = file - 'a';
            s.row = Character.getNumericValue(rank) - 1;
        } else {
            s.col = 0; s.row = 0;
        }
        return s;
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
