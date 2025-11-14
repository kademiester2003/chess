import client.ConsoleClient;

public class Main {
    public static void main(String[] args) throws Exception {
        ConsoleClient client = new ConsoleClient("localhost", "8080");
        client.run();
    }
}