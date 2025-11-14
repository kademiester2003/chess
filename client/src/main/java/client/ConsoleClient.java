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

    private void postloginLoop() throws IOException {}

    private String prompt(String prompt) throws IOException {
        System.out.print(prompt);
        return in.readLine();
    }

    private String promptNonEmpty(String prompt) throws IOException {
        String s = null;
        while (s == null || s.trim().isEmpty()) {
            s = prompt(prompt);
            if (s == null) {throw new IOException("Input closed")}
        }
        return s.trim();
    }
}
