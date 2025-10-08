package server;

import io.javalin.*;

public class Server {

    private final Javalin server;

    public Server() {
        server = Javalin.create(config -> config.staticFiles.add("web"));

        server.delete("db", ctx -> ctx.result( "{}"));
        server.post("user", ctx -> ctx.result("{\"username\":\"joe\", \"authToken\":\"xyz\"}"));

        // Register your endpoints and exception handlers here.

    }

    public int run(int desiredPort) {
        server.start(desiredPort);
        return server.port();
    }

    public void stop() {
        server.stop();
    }
}
