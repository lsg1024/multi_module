package com.msa.common.global.config;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.flyway.FlywayProperties;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@Profile("dev")
@Configuration
public class MetaDBConfig {

    @Bean("batchMetaDataSourceProperties")
    @ConfigurationProperties("meta-db")
    public DataSourceProperties batchMetaDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean("metaDataSource")
    public DataSource metaDataSource(
            @Qualifier("batchMetaDataSourceProperties") DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().build();
    }

    @Bean
    public PlatformTransactionManager metaTransactionManager(
            @Qualifier("metaDataSource") DataSource metaDataSource) {
        return new DataSourceTransactionManager(metaDataSource);
    }

    @Bean("batchFlywayProperties")
    @ConfigurationProperties("custom.flyway.batch")
    public FlywayProperties batchFlywayProperties() {
        return new FlywayProperties();
    }

    @Bean(initMethod = "migrate")
    public Flyway batchFlyway(
            @Qualifier("metaDataSource") DataSource metaDataSource,
            @Qualifier("batchFlywayProperties") FlywayProperties properties) {

        return Flyway.configure()
                .dataSource(metaDataSource)
                .locations(properties.getLocations().toArray(new String[0]))
                .baselineOnMigrate(properties.isBaselineOnMigrate())
                .load();
    }

}
