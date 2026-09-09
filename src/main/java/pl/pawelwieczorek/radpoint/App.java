package pl.pawelwieczorek.radpoint;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;

public final class App {

    private static final int PORT = 8080;

    private App() {
    }

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

        server.createContext("/data", App::handleData);
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Stopping server...");
            server.stop(0);
        }));

        server.start();
        System.out.println("Server started at http://localhost:" + PORT);
    }

    private static void handleData(HttpExchange exchange) throws IOException {
        String response = """
                {
                  "message": "Radpoint service is running"
                }
                """;

        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(200, response.getBytes(StandardCharsets.UTF_8).length);

        try (var output = exchange.getResponseBody()) {
            output.write(response.getBytes(StandardCharsets.UTF_8));
        }
    }
}