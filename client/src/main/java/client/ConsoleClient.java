package client;

import chess.ChessGame;
import client.ServerFacade.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

public class ConsoleClient {

    private final ServerFacade facade;
    private final BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

    private String authToken = null;
    private String loggedInUser = null;
    private List<GameEntry> lastGames = new ArrayList<>();

    public ConsoleClient(String serverHost, String serverPort) {
        facade = new ServerFacade(serverHost, serverPort);
    }

    public void run() throws IOException {
        System.out.println("Welcome to 240 chess. Type Help to get started.\n");

        while (true) {
            if (authToken == null) {preloginLoop();}
            else {postloginLoop();}
        }
    }

    private void preloginLoop() throws IOException {
        String line = prompt("[LOGGED_OUT] >>> ");
        if (line == null) {return;}

        String[] parts = line.trim().split("\\s+");
        if (parts.length == 0) {return;}
        String cmd = parts[0].toLowerCase();

        try {
            switch (cmd) {
                case "help" -> showPreloginHelp();
                case "quit" -> {
                    System.out.println("Goodbye.");
                    System.exit(0);
                }
                case "login" -> {
                    if (parts.length != 3) {
                        System.out.println("Wrong number of arguments.");
                        return;
                    }
                    handleLogin(parts[1], parts[2]);
                }
                case "register" -> {
                    if (parts.length != 4) {
                        System.out.println("Wrong number of arguments.");
                        return;
                    }
                    handleRegister(parts[1], parts[2], parts[3]);
                }
                default -> System.out.println("Unknown command. Type 'help' for options.");
            }
        } catch (Exception ex) {
            System.out.println("Error:" + ex.getMessage());
        }
    }

    private void showPreloginHelp() {
        System.out.println("""
              register <USERNAME> <PASSWORD> <EMAIL> - create an account
              login <USERNAME> <PASSWORD> - log into the server
              quit - quit the program
              help - list commands
            """);
    }

    private void handleLogin(String username, String password) throws IOException {
        try {
            LoginRequest req = new LoginRequest();
            req.username = username;
            req.password = password;

            LoginResponse r = facade.login(req);

            if (r.authToken != null) {
                authToken = r.authToken;
                loggedInUser = r.username;
                System.out.println("Logged in as " + loggedInUser + ".");
            } else {
                System.out.println("Login failed: " + (r.message == null ? "unknown error" : r.message));
            }

        } catch (Exception ex) {
            System.out.println("Error: " + ex.getMessage());
        }
    }

    private void handleRegister(String username, String password, String email) throws IOException {
        try {
            RegisterRequest req = new RegisterRequest();
            req.username = username;
            req.password = password;
            req.email = email;

            RegisterResponse r = facade.register(req);

            if (r.authToken != null) {
                authToken = r.authToken;
                loggedInUser = r.username;
                System.out.println("Registered and logged in as " + loggedInUser + ".");
            } else {
                System.out.println("Register failed: " + (r.message == null ? "unknown error" : r.message));
            }

        } catch (Exception ex) {
            System.out.println("Error: " + ex.getMessage());
        }
    }

    private void postloginLoop() throws IOException {
        String line = prompt("\n[LOGGED_IN] >>> ");
        if (line == null) {return;}

        String[] parts = line.trim().split("\\s+");
        if (parts.length == 0) {return;}
        String cmd = parts[0].toLowerCase();

        try {
            switch (cmd) {
                case "help" -> showPostloginHelp();
                case "logout" -> doLogout();
                case "create" -> {
                    if (parts.length != 2) {
                        System.out.println("Wrong number of arguments.");
                        return;
                    }
                    doCreateGame(String.join(" ", Arrays.copyOfRange(parts, 1, parts.length)));
                }
                case "list" -> doListGames();
                case "join" -> {
                    if (parts.length != 3) {
                        System.out.println("Wrong number of arguments.");
                        return;
                    }
                    doJoinGame(Integer.parseInt(parts[1]), parts[2]);
                }
                case "observe" -> {
                    if (parts.length != 2) {
                        System.out.println("Wrong number of arguments.");
                        return;
                    }
                    doObserveGame(Integer.parseInt(parts[1]));
                }
                case "quit" -> {
                    doLogout();
                    System.out.println("Goodbye.");
                    System.exit(0);
                }
                default -> System.out.println("Unknown command. Type 'help' for options.");
            }
        } catch (Exception ex) {
            System.out.println("Error: " + ex.getMessage());
        }
    }

    private void showPostloginHelp() {
        System.out.println("""
            create <NAME> - create a game
            list - list all games
            join <ID> [WHITE|BLACK] - join a game to play
            observe <ID> - join a game as an observer
            logout - log out of your account
            quit - exit the program
            help - list commands
        """);
    }

    private void doLogout() throws IOException {
        try {
            if (authToken == null) {
                System.out.println("Not logged in.");
                return;
            }

            LogoutResponse r = facade.logout(authToken);
            System.out.println(r.message != null ? r.message : "Logged out.");

        } catch (Exception ex) {
            System.out.println("Error logging out: " + ex.getMessage());
        } finally {
            authToken = null;
            loggedInUser = null;
            lastGames.clear();
        }
    }

    private void doCreateGame(String name) throws IOException {
        try {
            CreateGameRequest req = new CreateGameRequest(name);
            CreateGameResponse r = facade.createGame(req, authToken);

            if (r.gameID > 0) {
                System.out.println("Game created.");
            } else {
                System.out.println("Create failed: " + (r.message == null ? "unknown error" : r.message));
            }
        } catch (Exception ex) {
            System.out.println("Error creating game: " + ex.getMessage());
        }
    }

    private void doListGames() throws IOException {
        try {
            ListGamesResponse r = facade.listGames(authToken);

            if (r.message != null) {
                System.out.println("Error listing games: " + r.message);
                return;
            }

            lastGames = new ArrayList<>(Arrays.asList(r.games));

            if (lastGames.isEmpty()) {
                System.out.println("No games found.");
                return;
            }

            System.out.println("Games:");
            for (int i = 0; i < lastGames.size(); i++) {
                GameEntry g = lastGames.get(i);
                String players = (g.whiteUsername == null ? "<empty>" : g.whiteUsername)
                        + " vs "
                        + (g.blackUsername == null ? "<empty>" : g.blackUsername);
                System.out.printf("  %d) %s - %s%n", i + 1, g.gameName, players);
            }

        } catch (Exception ex) {
            System.out.println("Error listing games: " + ex.getMessage());
        }
    }

    private void doJoinGame(int gameNum, String color) throws IOException {
        try {
            if (lastGames.isEmpty()) {
                System.out.println("No recent game list. Use 'list' first.");
                return;
            }

            if (gameNum < 1 || gameNum > lastGames.size()) {
                System.out.println("Invalid number.");
                return;
            }

            GameEntry chosen = lastGames.get(gameNum - 1);

            String team = switch (color.toLowerCase()) {
                case "white", "w" -> "WHITE";
                case "black", "b" -> "BLACK";
                default -> "WHITE";
            };

            JoinGameRequest req = new JoinGameRequest(team, chosen.gameID);
            JoinGameResponse r = facade.joinGame(req, authToken);

            if (r.message != null) {
                System.out.println("Error: " + r.message);
                return;
            }

            if (team.equals("WHITE")) {
                chosen.whiteUsername = loggedInUser;
            } else {
                chosen.blackUsername = loggedInUser;
            }

            lastGames.set(gameNum - 1, chosen);

            System.out.println("Joined game.");

            boolean whitePerspective = !team.equalsIgnoreCase("BLACK");
            BoardDrawer.drawInitialBoard(
                    whitePerspective ? ChessGame.TeamColor.WHITE : ChessGame.TeamColor.BLACK);

        } catch (Exception ex) {
            System.out.println("Error joining game: " + ex.getMessage());
        }
    }

    private void doObserveGame(int gameNum) throws IOException {
        try {
            if (gameNum < 1 || gameNum > lastGames.size()) {
                System.out.println("Invalid number.");
                return;
            }

            GameEntry g = lastGames.get(gameNum - 1);
            System.out.println("Observing: " + g.gameName);
            BoardDrawer.drawInitialBoard(ChessGame.TeamColor.WHITE);

        } catch (Exception ex) {
            System.out.println("Error observing game: " + ex.getMessage());
        }
    }

    private String prompt(String p) throws IOException {
        System.out.print(p);
        return in.readLine();
    }
}