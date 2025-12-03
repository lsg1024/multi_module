package com.msa.product.global.batch.other;

import com.msa.product.local.classification.entity.Classification;
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
public class ClassificationUpdateBatchJob {

    @Bean
    public Job updateClassificationJob(JobRepository jobRepository, Step updateClassificationStep, UpdateJobListener updateJobListener) {
        return new JobBuilder("updateClassificationJob", jobRepository)
                .listener(updateJobListener)
                .start(updateClassificationStep)
                .build();
    }

    @Bean
    public Step updateClassificationStep(JobRepository jobRepository,
                                         PlatformTransactionManager transactionManager,
                                         @Qualifier("updateClassificationReader") JdbcPagingItemReader<Product> reader,
                                         @Qualifier("updateClassificationProcess") ItemProcessor<Product, Product> processor,
                                         @Qualifier("updateClassificationWriter") JdbcBatchItemWriter<Product> writer) {
        return new StepBuilder("updateClassificationStep", jobRepository)
                .<Product, Product>chunk(20, transactionManager)
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .transactionManager(transactionManager)
                .build();
    }

    @Bean
    @StepScope
    public JdbcPagingItemReader<Product> updateClassificationReader(
            @Value("#{jobParameters['tenantId']}") String tenantId,
            @Value("#{jobParameters['classificationId']}") String classificationId,
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
        queryProvider.setWhereClause("CLASSIFICATION_ID = :classificationId");
        queryProvider.setSortKeys(Map.of("PRODUCT_ID", Order.ASCENDING));

        reader.setQueryProvider(queryProvider);

        Map<String, Object> parameterValues = new HashMap<>();
        parameterValues.put("classificationId", classificationId);
        reader.setParameterValues(parameterValues);

        return reader;
    }

    @Bean
    @StepScope
    public ItemProcessor<Product, Product> updateClassificationProcess() {
        return product -> {
            product.setClassification(Classification.builder().classificationId(1L).build());
            return product;
        };
    }
    @Bean
    @StepScope
    public JdbcBatchItemWriter<Product> updateClassificationWriter(
            @Value("#{jobParameters['tenantId']}") String tenantId,
            @Qualifier("defaultDataSource") DataSource dataSource) {

        JdbcBatchItemWriter<Product> writer = new JdbcBatchItemWriter<>();
        writer.setDataSource(dataSource);
        writer.setSql("UPDATE " + tenantId + ".PRODUCT SET CLASSIFICATION_ID = :classification.classificationId WHERE PRODUCT_ID = :productId");
        writer.setItemSqlParameterSourceProvider(new BeanPropertyItemSqlParameterSourceProvider<>());
        writer.afterPropertiesSet();
        return writer;
    }
}
