package com.msa.product.global.batch;

import com.msa.product.local.product.entity.Product;
import com.msa.product.local.set.entity.SetType;
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
public class SetTypeUpdateBatchJob {

    @Bean
    public Job updateSetTypeUpdateJob(JobRepository jobRepository, Step updateSetTypeStep, UpdateJobListener listener) {
        return new JobBuilder("updateSetTypeJob", jobRepository)
                .listener(listener)
                .start(updateSetTypeStep)
                .build();
    }

    @Bean
    public Step updateSetTypeStep(JobRepository jobRepository,
                                  PlatformTransactionManager transactionManager,
                                  @Qualifier("updateSetTypeReader") JdbcPagingItemReader<Product> reader,
                                  @Qualifier("updateSetTypeProcess") ItemProcessor<Product, Product> processor,
                                  @Qualifier("updateSetTypeWriter") JdbcBatchItemWriter<Product> writer) {
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
    public JdbcPagingItemReader<Product> updateSetTypeReader(
            @Value("#{jobParameters['tenantId']}") String tenantId,
            @Value("#{jobParameters['setTypeId']}") String setTypeId,
            @Qualifier("defaultDataSource") DataSource dataSource) {

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
        queryProvider.setWhereClause("SET_TYPE_ID = :setTypeId");
        queryProvider.setSortKeys(Map.of("PRODUCT_ID", Order.ASCENDING));

        reader.setQueryProvider(queryProvider);

        Map<String, Object> parameterValues = new HashMap<>();
        parameterValues.put("setTypeId", setTypeId);
        reader.setParameterValues(parameterValues);

        return reader;
    }

    @Bean
    @StepScope
    public ItemProcessor<Product, Product> updateSetTypeProcess() {
        return product -> {
            product.setSetType(SetType.builder().setTypeId(1L).build());
            return product;
        };
    }
    @Bean
    @StepScope
    public JdbcBatchItemWriter<Product> updateSetTypeWriter(
            @Value("#{jobParameters['tenantId']}") String tenantId,
            @Qualifier("defaultDataSource") DataSource dataSource) {

        JdbcBatchItemWriter<Product> writer = new JdbcBatchItemWriter<>();
        writer.setDataSource(dataSource);
        writer.setSql("UPDATE " + tenantId +
                ".PRODUCT SET SET_TYPE_ID = :setType.setTypeId " +
                "WHERE PRODUCT_ID = :productId");
        writer.setItemSqlParameterSourceProvider(new BeanPropertyItemSqlParameterSourceProvider<>());
        writer.afterPropertiesSet();
        return writer;
    }

}
