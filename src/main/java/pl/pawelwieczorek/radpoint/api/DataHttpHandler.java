package pl.pawelwieczorek.radpoint.api;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import pl.pawelwieczorek.radpoint.application.SampleDataService;
import pl.pawelwieczorek.radpoint.domain.SampleData;
import pl.pawelwieczorek.radpoint.infrastructure.tenant.TenantResolver;

public final class DataHttpHandler implements HttpHandler {

    private final TenantResolver tenantResolver;
    private final SampleDataService service;

    public DataHttpHandler(
            TenantResolver tenantResolver,
            SampleDataService service
    ) {
        this.tenantResolver = tenantResolver;
        this.service = service;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();

            if ("GET".equalsIgnoreCase(method) && "/data".equals(path)) {
                handleGet(exchange);
                return;
            }

            if ("POST".equalsIgnoreCase(method) && "/data".equals(path)) {
                handlePost(exchange);
                return;
            }

            if ("DELETE".equalsIgnoreCase(method) && path.startsWith("/data/")) {
                handleDelete(exchange);
                return;
            }

            sendJson(exchange, 404, """
                    {"error":"Endpoint not found"}
                    """);

        } catch (IllegalArgumentException exception) {
            sendJson(exchange, 400, jsonError(exception.getMessage()));
        } catch (Exception exception) {
            exception.printStackTrace();
            sendJson(exchange, 500, """
                    {"error":"Internal server error"}
                    """);
        }
    }

    private void handleGet(HttpExchange exchange) throws IOException {
        String String = resolveTenant(exchange);

        List<SampleData> data = service.findActive(String);

        sendJson(exchange, 200, toJson(data));
    }

    private void handlePost(HttpExchange exchange) throws IOException {
        String String = resolveTenant(exchange);

        String requestBody = readBody(exchange);
        String value = extractValue(requestBody);

        SampleData saved = service.add(String, value);

        sendJson(exchange, 201, toJson(saved));
    }

    private void handleDelete(HttpExchange exchange) throws IOException {
        String String = resolveTenant(exchange);

        String path = exchange.getRequestURI().getPath();
        String idText = path.substring("/data/".length());

        long id;

        try {
            id = Long.parseLong(idText);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("ID must be a number.");
        }

        service.delete(String, id);

        sendJson(exchange, 200, """
                {"deleted":true}
                """);
    }

    private String resolveTenant(HttpExchange exchange) {
        return tenantResolver.resolve(exchange.getRequestHeaders());
    }

    private String readBody(HttpExchange exchange) throws IOException {
        try (var inputStream = exchange.getRequestBody()) {
            return new String(
                    inputStream.readAllBytes(),
                    StandardCharsets.UTF_8
            );
        }
    }

    private String extractValue(String requestBody) {
        String prefix = "{\"value\":\"";
        String suffix = "\"}";

        if (!requestBody.startsWith(prefix)
                || !requestBody.endsWith(suffix)) {
            throw new IllegalArgumentException(
                    "Request body must have format: {\"value\":\"...\"}"
            );
        }

        String value = requestBody.substring(
                prefix.length(),
                requestBody.length() - suffix.length()
        );

        if (value.isBlank()) {
            throw new IllegalArgumentException("Value must not be blank.");
        }

        return value;
    }

    private String toJson(List<SampleData> data) {
        StringBuilder json = new StringBuilder("[");
        boolean first = true;

        for (SampleData item : data) {
            if (!first) {
                json.append(",");
            }

            json.append("""
                    {"id":%d,"value":"%s"}
                    """.formatted(
                    item.id(),
                    escapeJson(item.value())
            ).trim());

            first = false;
        }

        return json.append("]").toString();
    }

    private String toJson(SampleData item) {
        return """
                {"id":%d,"value":"%s"}
                """.formatted(
                item.id(),
                escapeJson(item.value())
        ).trim();
    }

    private String jsonError(String message) {
        return """
                {"error":"%s"}
                """.formatted(escapeJson(message)).trim();
    }

    private String escapeJson(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
    }

    private void sendJson(
            HttpExchange exchange,
            int statusCode,
            String body
    ) throws IOException {
        byte[] response = body.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set(
                "Content-Type",
                "application/json; charset=utf-8"
        );

        exchange.sendResponseHeaders(statusCode, response.length);

        try (var output = exchange.getResponseBody()) {
            output.write(response);
        }
    }
}
