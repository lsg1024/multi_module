package com.msacommon.global.db;

import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
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

        if (DEFAULT_SCHEMA.equals(tenant)) {
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("SET SCHEMA " + DEFAULT_SCHEMA);
            }
            return connection;
        }

        try (Statement statement = connection.createStatement()) {
            // 동적 테넌트 스키마 생성 (없다면 생성)
            statement.execute("CREATE SCHEMA IF NOT EXISTS " + tenant);
        }

        if (initializedTenants.add(tenant)) {
            Flyway.configure()
                    .dataSource(defaultDataSource)
                    .schemas(tenant)
                    .locations("classpath:db/migration")
                    .baselineOnMigrate(true)
                    .load()
                    .migrate();
            log.info("Flyway migration applied for tenant schema: {}", tenant);
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

    @Override public boolean supportsAggressiveRelease() { return false; }
    @Override public boolean isUnwrappableAs(Class unwrapType) { return false; }
    @Override public <T> T unwrap(Class<T> unwrapType) { return null; }
}
