package pl.pawelwieczorek.radpoint.api;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import pl.pawelwieczorek.radpoint.application.SampleDataService;
import pl.pawelwieczorek.radpoint.domain.SampleData;
import pl.pawelwieczorek.radpoint.infrastructure.tenant.TenantResolver;

public final class DataHttpHandler implements HttpHandler {
	
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    
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

            sendJson(exchange, 404, toJson(Map.of("error", "Endpoint not found")));

        } catch (IllegalArgumentException exception) {
            sendJson(exchange, 400, toJson(Map.of("error", exception.getMessage())));
        } catch (Exception exception) {
            exception.printStackTrace();
            sendJson(exchange, 500, toJson(Map.of("error", "Internal server error")));
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
        try {
            JsonNode root = OBJECT_MAPPER.readTree(requestBody);

            if (root == null || !root.isObject()) {
                throw new IllegalArgumentException(
                        "Request body must be a JSON object."
                );
            }

            JsonNode valueNode = root.get("value");

            if (valueNode == null || !valueNode.isTextual()) {
                throw new IllegalArgumentException(
                        "Request body must contain a text field: value."
                );
            }

            String value = valueNode.asText().trim();

            if (value.isBlank()) {
                throw new IllegalArgumentException(
                        "Value must not be blank."
                );
            }

            return value;
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException(
                    "Request body must contain valid JSON.",
                    exception
            );
        }
    }
    
    private String toJson(Object value) {
        try {
            return OBJECT_MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "Could not serialize JSON response.",
                    exception
            );
        }
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
