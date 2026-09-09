package pl.pawelwieczorek.radpoint;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

import com.sun.net.httpserver.HttpServer;

import pl.pawelwieczorek.radpoint.api.DataHttpHandler;
import pl.pawelwieczorek.radpoint.application.SampleDataRepository;
import pl.pawelwieczorek.radpoint.application.SampleDataService;
import pl.pawelwieczorek.radpoint.infrastructure.sqlite.SqliteSampleDataRepository;
import pl.pawelwieczorek.radpoint.infrastructure.sqlite.TenantConnectionProvider;
import pl.pawelwieczorek.radpoint.infrastructure.tenant.TenantResolver;

public final class App {

	private static final int PORT = 8080;

	private App() {
	}

	public static void main(String[] args) throws IOException {
		HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

		TenantResolver tenantResolver = new TenantResolver();

		TenantConnectionProvider connectionProvider =
		        new TenantConnectionProvider();

		SampleDataRepository repository =
		        new SqliteSampleDataRepository(connectionProvider);

		SampleDataService service =
		        new SampleDataService(repository);

		server.createContext(
		        "/data",
		        new DataHttpHandler(tenantResolver, service)
		);
		server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());

		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			System.out.println("Stopping server...");
			connectionProvider.close();
			server.stop(0);
		}));

		server.start();
		System.out.println("Server started at http://localhost:" + PORT);
	}
}