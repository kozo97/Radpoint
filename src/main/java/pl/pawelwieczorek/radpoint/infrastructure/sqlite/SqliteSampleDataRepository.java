package pl.pawelwieczorek.radpoint.infrastructure.sqlite;


import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import pl.pawelwieczorek.radpoint.application.SampleDataRepository;
import pl.pawelwieczorek.radpoint.domain.SampleData;

public final class SqliteSampleDataRepository implements SampleDataRepository {

    private final TenantConnectionProvider connectionProvider;

    public SqliteSampleDataRepository(TenantConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    @Override
    public List<SampleData> findActive(String tenantId) {
        String sql = """
                SELECT id, value
                FROM sample_data
                WHERE deleted = 0
                ORDER BY id
                """;

        List<SampleData> result = new ArrayList<>();

        try {
            Connection connection = connectionProvider.getConnection(tenantId);

            try (PreparedStatement statement = connection.prepareStatement(sql);
                 ResultSet resultSet = statement.executeQuery()) {

                while (resultSet.next()) {
                    result.add(new SampleData(
                            resultSet.getLong("id"),
                            resultSet.getString("value")
                    ));
                }
            }

            return result;
        } catch (SQLException exception) {
            throw new IllegalStateException(
                    "Could not read data for tenant: " + tenantId,
                    exception
            );
        }
    }

    @Override
    public SampleData save(String tenantId, String value) {
        String insertSql = """
                INSERT INTO sample_data (value, deleted)
                VALUES (?, 0)
                """;

        try {
            Connection connection = connectionProvider.getConnection(tenantId);
            long id;

            try (PreparedStatement statement = connection.prepareStatement(insertSql)) {
                statement.setString(1, value);
                statement.executeUpdate();
            }

            try (var statement = connection.createStatement();
                 var resultSet = statement.executeQuery(
                         "SELECT last_insert_rowid()"
                 )) {

                if (!resultSet.next()) {
                    throw new SQLException("Could not determine inserted ID.");
                }

                id = resultSet.getLong(1);
            }

            return new SampleData(id, value);
        } catch (SQLException exception) {
            throw new IllegalStateException(
                    "Could not save data for tenant: " + tenantId,
                    exception
            );
        }
    }

    @Override
    public boolean softDelete(String tenantId, long id) {
        String sql = """
                UPDATE sample_data
                SET deleted = 1
                WHERE id = ?
                  AND deleted = 0
                """;

        try {
            Connection connection = connectionProvider.getConnection(tenantId);

            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setLong(1, id);
                return statement.executeUpdate() == 1;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException(
                    "Could not delete data for tenant: " + tenantId,
                    exception
            );
        }
    }
}
