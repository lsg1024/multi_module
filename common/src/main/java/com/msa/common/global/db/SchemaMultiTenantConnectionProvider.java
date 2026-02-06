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
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@NoTrace
@Profile("dev")
@Component
@ManagedResource(objectName="com.msa:type=MultiTenant,bean=SchemaProvider")
public class SchemaMultiTenantConnectionProvider implements MultiTenantConnectionProvider {

    private final DataSource defaultDataSource;
    private final Set<String> initializedTenants = ConcurrentHashMap.newKeySet();
    private final Map<String, Object> tenantLocks = new ConcurrentHashMap<>();
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
        String tenant = tenantIdentifier.toString().toLowerCase();

        if (!isValidTenantIdentifier(tenant)) {
            throw new SQLException("Invalid tenant identifier: " + tenant);
        }

        // 마이그레이션은 커넥션 점유 없이 실행 (풀 고갈 방지)
        initializeTenantIfNeeded(tenant);

        // 마이그레이션 완료 후 커넥션 획득
        Connection connection = getAnyConnection();
        try {
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("SET search_path TO " + tenant);
            }
            return connection;
        } catch (Exception e) {
            try { connection.close(); } catch (SQLException ignored) {}
            if (e instanceof SQLException) throw (SQLException) e;
            throw new SQLException("Failed to get connection for tenant", e);
        }
    }

    /**
     * 테넌트 초기화 (스키마 생성 + Flyway 마이그레이션)
     * - 커넥션을 점유하지 않은 상태에서 실행하여 풀 고갈 방지
     * - 테넌트별 synchronized로 동시 마이그레이션 방지
     */
    private void initializeTenantIfNeeded(String tenant) throws SQLException {
        if (initializedTenants.contains(tenant)) {
            return;
        }

        synchronized (tenantLocks.computeIfAbsent(tenant, k -> new Object())) {
            if (initializedTenants.contains(tenant)) {
                return;
            }

            try (Connection conn = getAnyConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.execute("CREATE SCHEMA IF NOT EXISTS " + tenant);
            }

            runMigration(tenant);
            initializedTenants.add(tenant);
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
