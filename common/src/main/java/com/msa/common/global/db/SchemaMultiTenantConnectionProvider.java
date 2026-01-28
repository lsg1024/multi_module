package com.msa.common.global.db;

import com.msa.common.global.aop.NoTrace;
import org.flywaydb.core.Flyway;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.*;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@NoTrace
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
        try {
            String tenant = tenantIdentifier.toString().toLowerCase();

            if (!isValidTenantIdentifier(tenant)) {
                throw new SQLException("Invalid tenant identifier: " + tenant);
            }

            if (!initializedTenants.contains(tenant)) {
                try (Statement stmt = connection.createStatement()) {
                    stmt.execute("CREATE SCHEMA IF NOT EXISTS " + tenant);
                }
                runMigration(tenant);
                initializedTenants.add(tenant);
            }

            try (Statement stmt = connection.createStatement()) {
                stmt.execute("SET search_path TO " + tenant);
            }

            return connection;
        } catch (Exception e) {
            // 예외 발생 시 커넥션 반환하여 누수 방지
            try {
                connection.close();
            } catch (SQLException closeEx) {
                // 닫기 실패는 무시
            }
            if (e instanceof SQLException) {
                throw (SQLException) e;
            }
            throw new SQLException("Failed to get connection for tenant", e);
        }
    }

    @Override
    public void releaseConnection(Object tenantIdentifier, Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            // 기본 스키마로 복귀
            statement.execute("SET search_path TO " + DEFAULT_SCHEMA);
        }
        connection.close();
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

    private boolean isValidTenantIdentifier(String tenant) {
        return tenant != null && tenant.matches("^[a-zA-Z0-9_]+$");
    }
}
