package com.msa.product.global.batch;

import com.msa.product.local.material.entity.Material;
import com.msa.product.local.product.entity.Product;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.BeanPropertyItemSqlParameterSourceProvider;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.JdbcPagingItemReader;
import org.springframework.batch.item.database.Order;
import org.springframework.batch.item.database.support.MySqlPagingQueryProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class MaterialUpdateBatchJob {

    @Bean
    public Job updateMaterialUpdateJob(JobRepository jobRepository, Step updateMaterialStep, UpdateJobListener listener) {
        return new JobBuilder("updateMaterialJob", jobRepository)
                .listener(listener)
                .start(updateMaterialStep)
                .build();
    }

    @Bean
    public Step updateMaterialStep(JobRepository jobRepository,
                                   PlatformTransactionManager transactionManager,
                                   @Qualifier("updateMaterialReader") JdbcPagingItemReader<Product> reader,
                                   @Qualifier("updateMaterialProcess") ItemProcessor<Product, Product> processor,
                                   @Qualifier("updateMaterialWriter") JdbcBatchItemWriter<Product> writer) {
        return new StepBuilder("updateMaterialStep", jobRepository)
                .<Product, Product>chunk(20, transactionManager)
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .transactionManager(transactionManager)
                .build();
    }

    @Bean
    @StepScope
    public JdbcPagingItemReader<Product> updateMaterialReader(
            @Value("#{jobParameters['tenantId']}") String tenantId,
            @Value("#{jobParameters['materialId']}") String materialId,
            @Qualifier("defaultDataSource")DataSource dataSource) {

        JdbcPagingItemReader<Product> reader = new JdbcPagingItemReader<>();
        reader.setDataSource(dataSource);
        reader.setPageSize(20);

        reader.setRowMapper((rs, rowNum) -> Product.builder()
                .productId(rs.getLong("PRODUCT_ID"))
                .build()
        );
        MySqlPagingQueryProvider queryProvider = new MySqlPagingQueryProvider();
        queryProvider.setSelectClause("SELECT *");
        queryProvider.setFromClause(tenantId + ".PRODUCT");
        queryProvider.setWhereClause("MATERIAL_ID = :materialId");
        queryProvider.setSortKeys(Map.of("PRODUCT_ID", Order.ASCENDING));

        reader.setQueryProvider(queryProvider);

        Map<String, Object> parameterValues = new HashMap<>();
        parameterValues.put("materialId", materialId);
        reader.setParameterValues(parameterValues);

        return reader;
    }

    @Bean
    @StepScope
    public ItemProcessor<Product, Product> updateMaterialProcess() {
        return product -> {
            product.setMaterial(Material.builder().materialId(1L).build());
            return product;
        };
    }
    @Bean
    @StepScope
    public JdbcBatchItemWriter<Product> updateMaterialWriter(
            @Value("#{jobParameters['tenantId']}") String tenantId,
            @Qualifier("defaultDataSource") DataSource dataSource) {

        JdbcBatchItemWriter<Product> writer = new JdbcBatchItemWriter<>();
        writer.setDataSource(dataSource);
        writer.setSql("UPDATE " + tenantId + ".PRODUCT SET MATERIAL_ID = :material.materialId WHERE PRODUCT_ID = :productId");
        writer.setItemSqlParameterSourceProvider(new BeanPropertyItemSqlParameterSourceProvider<>());
        writer.afterPropertiesSet();
        return writer;
    }
}
