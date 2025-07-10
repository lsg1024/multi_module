package com.msacommon.global.db;

import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@ManagedResource(objectName="com.msa:type=MultiTenant,bean=SchemaProvider")
public class SchemaMultiTenantConnectionProvider implements MultiTenantConnectionProvider {

    private final DataSource defaultDataSource;
    private final Set<String> initializedTenants = ConcurrentHashMap.newKeySet();
    private static final String DEFAULT_SCHEMA = "PUBLIC";

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
        String tenant = tenantIdentifier.toString().toUpperCase();

        log.info("initializedTenants: {}", initializedTenants);

        if (DEFAULT_SCHEMA.equals(tenant)) {
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("SET SCHEMA " + DEFAULT_SCHEMA);
            }
            return connection;
        }

        try (Statement stmt = connection.createStatement()) {
            stmt.execute("CREATE SCHEMA IF NOT EXISTS " + tenant);
        }

        if (!initializedTenants.contains(tenant)) {
            initializedTenants.add(tenant);
            runMigration(tenant);
        } else {
            if (isSchemaEmpty(connection, tenant)) {
                log.warn("Schema '{}' previously initialized but now empty. Re-running Flyway migration.", tenant);
                runMigration(tenant);
            }
        }

        // 스키마 전환
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("SET SCHEMA " + tenant);
            log.info("Switched to tenant schema: {}", tenant);
        }

        return connection;
    }

    @Override
    public void releaseConnection(Object tenantIdentifier, Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            // 기본 스키마로 복귀
            statement.execute("SET SCHEMA PUBLIC");
        }
        connection.close();
    }

    private boolean isSchemaEmpty(Connection conn, String tenant) {
        String sql = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = '" + tenant + "'";
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                int count = rs.getInt(1);
                log.debug("Schema '{}' has {} tables.", tenant, count);
                return count == 0;
            }
        } catch (SQLException e) {
            log.warn("Failed to check if schema '{}' is empty: {}", tenant, e.getMessage());
        }
        return false;
    }

    private void runMigration(String tenant) {
        Flyway.configure()
                .dataSource(defaultDataSource)
                .schemas(tenant)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .load()
                .migrate();

        log.info("Flyway migration completed for tenant '{}'", tenant);
    }
    @Override public boolean supportsAggressiveRelease() { return false; }
    @Override public boolean isUnwrappableAs(Class unwrapType) { return false; }

    @Override public <T> T unwrap(Class<T> unwrapType) { return null; }
}
