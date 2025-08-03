package com.msa.common.global.config;

import com.msa.common.global.db.DynamicDataSourceRouter;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;

@Profile("dev")
@Configuration
public class DataSourceConfig {

    @Bean
    @Qualifier("defaultDataSource")
    public DataSource defaultDataSource(DataSourceProperties properties) {
        HikariDataSource ds = properties
                .initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();

        ds.setConnectionInitSql("SET SCHEMA PUBLIC");
        return ds;
    }

    @Bean
    public DataSource dataSource(
            @Qualifier("defaultDataSource") DataSource defaultDs) {
        return new DynamicDataSourceRouter(defaultDs);
    }
}
