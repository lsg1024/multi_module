package com.msacommon.global.db;

import com.msacommon.global.tenant.TenantContext;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
@Profile("dev")
@Component
public class DynamicDataSourceRouter extends AbstractRoutingDataSource {

    private final DataSource defaultDs;
    private final Map<String, DataSource> tenantDsMap = new ConcurrentHashMap<>();

    public DynamicDataSourceRouter(
            @Qualifier("defaultDataSource") DataSource defaultDs) {
        this.defaultDs = defaultDs;
        super.setDefaultTargetDataSource(defaultDs);
        super.setTargetDataSources(new HashMap<>());
        super.afterPropertiesSet();
    }

    @Override
    protected Object determineCurrentLookupKey() {
        return TenantContext.getTenant();
    }

    @Override
    protected DataSource determineTargetDataSource() {
        String tenantId = (String) determineCurrentLookupKey();

        if (tenantId == null || tenantId.isEmpty()) {
            return defaultDs;
        }

        return tenantDsMap.computeIfAbsent(tenantId, this::createAndRegisterTenantDs);
    }

    private synchronized DataSource createAndRegisterTenantDs(String tenantId) {
        // 스키마 생성
        try (Connection conn = defaultDs.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE SCHEMA IF NOT EXISTS " + tenantId);
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to create schema: " + tenantId, e);
        }

        Flyway.configure()
                .dataSource(defaultDs)
                .schemas(tenantId)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .load()
                .migrate();

        // 라우팅 맵 업데이트
        Map<Object, Object> targetDataSources = new HashMap<>(this.tenantDsMap);
        targetDataSources.put(tenantId, defaultDs);
        super.setTargetDataSources(targetDataSources);
        super.afterPropertiesSet();

        return defaultDs;
    }
}
