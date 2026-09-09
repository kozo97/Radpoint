package pl.pawelwieczorek.radpoint;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import pl.pawelwieczorek.radpoint.application.SampleDataRepository;
import pl.pawelwieczorek.radpoint.application.SampleDataService;
import pl.pawelwieczorek.radpoint.infrastructure.sqlite.SqliteSampleDataRepository;
import pl.pawelwieczorek.radpoint.infrastructure.sqlite.TenantConnectionProvider;
import pl.pawelwieczorek.radpoint.infrastructure.tenant.TenantResolver;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;

public final class App {

	private static final int PORT = 8080;

	private static final TenantResolver TENANT_RESOLVER = new TenantResolver();

	private static final TenantConnectionProvider CONNECTION_PROVIDER = new TenantConnectionProvider();

	private static final SampleDataRepository REPOSITORY = new SqliteSampleDataRepository(CONNECTION_PROVIDER);

	private static final SampleDataService SERVICE = new SampleDataService(REPOSITORY);

	private App() {
	}

	public static void main(String[] args) throws IOException {
		HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

		server.createContext("/data", App::handleData);
		server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());

		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			System.out.println("Stopping server...");
			CONNECTION_PROVIDER.close();
			server.stop(0);
		}));

		server.start();
		System.out.println("Server started at http://localhost:" + PORT);
	}

	private static void handleData(HttpExchange exchange) throws IOException {
		try {
			String tenantId = TENANT_RESOLVER.resolve(exchange.getRequestHeaders());
			CONNECTION_PROVIDER.getConnection(tenantId);

			String response = """
					{
					  "tenantId": "%s",
					  "message": "Tenant resolved successfully"
					}
					""".formatted(tenantId);

			sendJson(exchange, 200, response);
		} catch (IllegalArgumentException exception) {
			String response = """
					{
					  "error": "%s"
					}
					""".formatted(exception.getMessage());

			sendJson(exchange, 400, response);
		}
	}

	private static void sendJson(HttpExchange exchange, int statusCode, String response) throws IOException {
		byte[] body = response.getBytes(StandardCharsets.UTF_8);

		exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
		exchange.sendResponseHeaders(statusCode, body.length);

		try (var output = exchange.getResponseBody()) {
			output.write(body);
		}
	}
}