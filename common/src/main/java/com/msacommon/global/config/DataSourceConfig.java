package com.msacommon.global.config;

import com.msacommon.global.db.DynamicDataSourceRouter;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

@Configuration
@EnableConfigurationProperties(DataSourceProperties.class)
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

    @Bean(name = "flywayDataSource")
    public DataSource flywayDataSource(@Qualifier("defaultDataSource") DataSource defaultDs) {
        return defaultDs;
    }

    @Bean
    @Primary
    public DataSource dataSource(@Qualifier("defaultDataSource") DataSource defaultDs) {
        return new DynamicDataSourceRouter(defaultDs);
    }
}
