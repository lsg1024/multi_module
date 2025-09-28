package com.msa.common.global.db;

import org.flywaydb.core.Flyway;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Profile("dev")
@Component
@ManagedResource(objectName="com.msa:type=MultiTenant,bean=SchemaProvider")
public class SchemaMultiTenantConnectionProvider implements MultiTenantConnectionProvider {

    private final DataSource defaultDataSource;
    private final Set<String> initializedTenants = ConcurrentHashMap.newKeySet();
    private static final String DEFAULT_SCHEMA = "public";

    public SchemaMultiTenantConnectionProvider(@Qualifier("defaultDataSource") DataSource defaultDataSource) {
        this.defaultDataSource = defaultDataSource;
    }

    @Override
    public Connection getAnyConnection() throws SQLException {
        return defaultDataSource.getConnection();
    }

    @Override
    public void releaseAnyConnection(Connection connection) throws SQLException {
        connection.close();
    }

    @Override
    public Connection getConnection(Object tenantIdentifier) throws SQLException {
        Connection connection = getAnyConnection();
        String tenant = tenantIdentifier.toString().toLowerCase();

        if (!schemaExists(connection, tenant)) {
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("CREATE SCHEMA " + tenant);
            }
            runMigration(tenant);
        }

        try (Statement stmt = connection.createStatement()) {
            stmt.execute("SET search_path TO " + tenant);
        }

        return connection;
    }

    @Override
    public void releaseConnection(Object tenantIdentifier, Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            // 기본 스키마로 복귀
            statement.execute("SET search_path TO " + DEFAULT_SCHEMA);
        }
        connection.close();
    }

    private boolean isSchemaEmpty(Connection conn, String tenant) {
        String sql = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = '" + tenant + "'";
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                int count = rs.getInt(1);
                return count == 0;
            }
        } catch (SQLException e) {
        }
        return false;
    }

    private boolean schemaExists(Connection connection, String schema) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT 1 FROM pg_namespace WHERE nspname = '" + schema + "'");
            return rs.next();
        }
    }

    private void runMigration(String tenant) {
        Flyway.configure()
                .dataSource(defaultDataSource)
                .schemas(tenant)
                .initSql(String.format("SET search_path TO '%s'", tenant))
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .load()
                .migrate();
    }
    @Override public boolean supportsAggressiveRelease() { return false; }
    @Override public boolean isUnwrappableAs(Class unwrapType) { return false; }

    @Override public <T> T unwrap(Class<T> unwrapType) { return null; }
}
