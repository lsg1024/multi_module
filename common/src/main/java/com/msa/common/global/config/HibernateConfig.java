package com.msa.common.global.config;

import com.msa.common.global.db.CurrentTenantIdentifierResolverImpl;
import com.msa.common.global.db.SchemaMultiTenantConnectionProvider;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.cfg.AvailableSettings;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Profile("dev")
@Configuration
@ConfigurationProperties(prefix = "custom.jpa")
public class HibernateConfig {

    private List<String> entityScanPackages;

    public List<String> getEntityScanPackages() {
        return entityScanPackages;
    }

    public void setEntityScanPackages(List<String> entityScanPackages) {
        this.entityScanPackages = entityScanPackages;
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(
            @Qualifier("dataSource") DataSource routingDataSource,
            SchemaMultiTenantConnectionProvider connectionProvider,
            CurrentTenantIdentifierResolverImpl resolver) {

        LocalContainerEntityManagerFactoryBean emf = new LocalContainerEntityManagerFactoryBean();
        emf.setDataSource(routingDataSource);
        emf.setPackagesToScan(entityScanPackages.toArray(new String[0]));
        emf.setJpaVendorAdapter(new HibernateJpaVendorAdapter());

        Map<String, Object> props = new HashMap<>();
        props.put(AvailableSettings.MULTI_TENANT_CONNECTION_PROVIDER, connectionProvider);
        props.put(AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER, resolver);
        emf.setJpaPropertyMap(props);

        return emf;
    }

    @Primary
    @Bean
    public PlatformTransactionManager transactionManager(EntityManagerFactory emf) {
        return new JpaTransactionManager(emf);
    }
}
