package client;

import com.google.gson.Gson;
import client.ServerFacade.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

public class ConsoleClient {
    private final ServerFacade facade;
    private final BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
    private final Gson gson = new Gson();
    private String authToken = null;
    private String loggedInUser = null;
    private List<GameEntry> lastGames = new ArrayList<>();

    public ConsoleClient(String serverHost, int serverPort) {
        facade = new ServerFacade(serverHost, serverPort);
    }

    public static void main(String[] args) throws Exception {
        ConsoleClient client = new ConsoleClient("localhost", 7000);
        client.run();
    }

    public void run() throws IOException {
        System.out.println("Welcome to Console Chess (Phase 5 pregame client).");
        while (true) {
            if (authToken == null) {
                preloginLoop();
            } else {
                postloginLoop();
            }
        }
    }

    private void preloginLoop() throws IOException {
        System.out.println("\nprelogin> (type 'help' for commands)");
        String line = prompt("> ");
        if (line == null) {return;}
        switch (line.trim().toLowerCase(Locale.ROOT)) {
            case "help" -> showPreloginHelp();
            case "quit", "exit" -> {System.out.println("Goodbye."); System.exit(0);}
            case "login" -> handleLogin();
            case "register" -> handleRegister();
            default -> System.out.println("Unknown command. Type 'help' for options.");
        }
    }

    private void showPreloginHelp() {
        System.out.println("""
                Prelogin commands:
                  help      - show this message
                  register  - create a new user and log in
                  login     - log in with existing user
                  quit/exit - exit the program
                """);
    }

    private void handleLogin() throws IOException {
        try {
            String username = promptNonEmpty("username: ");
            String password = promptNonEmpty("password: ");
            LoginResponse r = facade.login(username, password);
            if (r.authToken != null) {
                authToken = r.authToken;
                loggedInUser = username;
                System.out.println("Logged in as " + username + ".");
            } else {
                System.out.println("Login failed: " + r.username);
            }
        } catch (Exception ex) {
            System.out.println("Error: " + ex.getMessage());
        }
    }

    private void handleRegister() throws IOException {
        try {
            String username = promptNonEmpty("username: ");
            String password = promptNonEmpty("password; ");
            String email = promptNonEmpty("email; ");
            RegisterResponse r = facade.register(username, password, email);

            if (r.authToken != null) {
                authToken = r.authToken;
                loggedInUser = username;
                System.out.println("Registered and logged in as " + username + ".");
            }
            else {
                System.out.println("Register failed: " + r.username);
            }
        } catch (Exception ex) {
            System.out.println("Error: " + ex.getMessage());
        }
    }

    private void postloginLoop() throws IOException {
        System.out.println("\npostlogin> (type 'help' for commands)");
        String line = prompt("> ");
        if (line == null) {return;}
        switch (line.trim().toLowerCase(Locale.ROOT)) {
            case "help" -> showPostloginHelp();
            case "logout" -> doLogout();
            case "create" -> doCreateGame();
            case "list" -> doListGames();
            case "play" -> doPlayGame();
            case "observe" -> doObserveGame();
            case "quit", "exit" -> {
                doLogout();
                System.out.println("Goodbye.");
                System.exit(0);
            }
            default -> System.out.println("Unknown command. Type 'help' for options.");
        }
    }

    private void showPostloginHelp() {
        System.out.println("""
                Postlogin commands:
                  help      - show this message
                  list      - list games on server
                  create    - create a new game (does NOT join)
                  play      - join a game and draw board (no gameplay yet)
                  observe   - observe a game (draw board)
                  logout    - log out
                  quit/exit - logout and exit
        """);
    }

    private void doLogout() throws IOException {
        try {
            if (authToken == null) {
                System.out.println("Not logged in.");
                return;
            }
            GenericResponse r = facade.logout(authToken);
            System.out.println(r != null && r.message != null ? r.message : "Logged out.");
        }  catch (Exception ex) {
            System.out.println("Error logging out: " + ex.getMessage());
        } finally {
            authToken = null;
            loggedInUser = null;
            lastGames.clear();
        }
    }

    private void doCreateGame() throws IOException {
        try {
            String name = promptNonEmpty("Game name: ");
            CreateGameResponse r = facade.createGame(name, authToken);
            if (r.gameID > 0) {
                System.out.println("Game created.");
            } else {
                System.out.println("Create failed: " + (r.message == null ? "unknown" : r.message));
            }
        } catch (Exception ex) {
            System.out.println("Error creating game: " + ex.getMessage());
        }
    }

    private void doListGames() throws IOException {
        try {
            List<GameEntry> games = facade.listGames(authToken);
            lastGames = new ArrayList<>(games);
            if (games.isEmpty()) {
                System.out.println("No games found.");
                return;
            }
            System.out.println("Games:");
            for (int i = 0; i < games.size(); i++) {
                GameEntry g = games.get(i);
                String players = String.format("%s vs %s", g.whiteUsername == null ? "<empty>" : g.whiteUsername, g.blackUsername == null ? "<empty>" : g.blackUsername);
                System.out.printf("  %d) %s - %s%n", i + 1, g.gameName, players);
            }
        } catch (Exception ex) {
            System.out.println("Error listing games: " + ex.getMessage());
        }
    }

    private void doPlayGame() throws IOException {
        try {
            if (lastGames.isEmpty()) {
                System.out.println("No recent game list. Use 'list' first.");
                return;
            }
            String idxStr = promptNonEmpty("Enter game number to join (from last 'list'): ");
            int idx = Integer.parseInt(idxStr);
            if (idx < 1 || idx > lastGames.size()) {
                System.out.println("Invalid number.");
                return;
            }
            GameEntry chosen = lastGames.get(idx - 1);
            String color = promptNonEmpty("Which color do you want to play? (white/black): ").trim().toLowerCase(Locale.ROOT);
            String team = switch(color) {
                case "white", "w" -> "WHITE";
                case "black", "b" -> "BLACK";
                default -> { System.out.println("Unknown color - using white by default."); yield "WHITE"; }
            };

            GenericResponse r = facade.joinGame(team, chosen.gameID, authToken);
            System.out.println(r != null && r.message != null ? r.message : "Joined (server did not return a message).");

            boolean drawWhitePerspective = !team.equals("BLACK");
            BoardDrawer.drawInitialBoard(drawWhitePerspective);
        } catch (NumberFormatException nf) {
            System.out.println("Please enter a valid number.");
        } catch (Exception ex) {
            System.out.println("Error joining game: " + ex.getMessage());
        }
    }

    private void doObserveGame() throws IOException {
        try {
            if (lastGames.isEmpty()) {
                System.out.println("No recent game list. Use 'list' first.");
                return;
            }
            String idxStr = promptNonEmpty("Enter game number to observe (from last 'list'): ");
            int idx = Integer.parseInt(idxStr);
            if (idx < 1 || idx > lastGames.size()) {
                System.out.println("Invalid number.");
                return;
            }
            GameEntry chosen = lastGames.get(idx - 1);
            System.out.println("Observing: " +  chosen.gameName + " (" + (chosen.whiteUsername == null ? "<empty>" : chosen.whiteUsername) + " vs " + (chosen.blackUsername == null ? "<empty>" : chosen.blackUsername) + ")");
            BoardDrawer.drawinitialBoard(true);
        } catch (NumberFormatException nf) {
            System.out.println("Please enter a valid number.");
        } catch (Exception ex) {
            System.out.println("Error observing game: " + ex.getMessage());
        }
    }

    private String prompt(String prompt) throws IOException {
        System.out.print(prompt);
        return in.readLine();
    }

    private String promptNonEmpty(String prompt) throws IOException {
        String s = null;
        while (s == null || s.trim().isEmpty()) {
            s = prompt(prompt);
            if (s == null) {throw new IOException("Input closed");}
        }
        return s.trim();
    }
}
