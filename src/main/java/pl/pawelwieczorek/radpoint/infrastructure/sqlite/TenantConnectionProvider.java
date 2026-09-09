package pl.pawelwieczorek.radpoint.infrastructure.sqlite;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class TenantConnectionProvider implements AutoCloseable {

    private static final Path TENANTS_DIRECTORY = Path.of("tenants");

    private static final String CREATE_SAMPLE_DATA_TABLE = """
            CREATE TABLE IF NOT EXISTS sample_data (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                value TEXT NOT NULL,
                deleted INTEGER NOT NULL DEFAULT 0
            )
            """;

    private final Map<String, Connection> connections = new ConcurrentHashMap<>();

    public Connection getConnection(String tenantId) {
        return connections.computeIfAbsent(tenantId, this::openAndInitialize);
    }

    private Connection openAndInitialize(String tenantId) {
        try {
            Files.createDirectories(TENANTS_DIRECTORY);

            Path databaseFile = TENANTS_DIRECTORY.resolve(tenantId + ".db");

            Connection connection = DriverManager.getConnection(
                    "jdbc:sqlite:" + databaseFile.toAbsolutePath()
            );

            try (Statement statement = connection.createStatement()) {
                statement.execute(CREATE_SAMPLE_DATA_TABLE);
            }

            return connection;
        } catch (SQLException | IOException exception) {
            throw new IllegalStateException(
                    "Could not open database for tenant: " + tenantId,
                    exception
            );
        }
    }

    @Override
    public void close() {
        connections.values().forEach(connection -> {
            try {
                connection.close();
            } catch (SQLException exception) {
                System.err.println("Could not close SQLite connection: " + exception.getMessage());
            }
        });

        connections.clear();
    }
}
