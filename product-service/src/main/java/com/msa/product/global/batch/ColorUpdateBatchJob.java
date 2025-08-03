package com.msa.product.global.batch;

import com.msa.product.local.color.entity.Color;
import com.msa.product.local.product.entity.ProductWorkGradePolicyGroup;
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
public class ColorUpdateBatchJob {

    @Bean
    public Job updateColorJob(JobRepository jobRepository, Step updateColorStep, UpdateJobListener listener) {
        return new JobBuilder("updateColorJob", jobRepository)
                .listener(listener)
                .start(updateColorStep)
                .build();
    }

    @Bean
    public Step updateColorStep(JobRepository jobRepository,
                                PlatformTransactionManager transactionManager,
                                @Qualifier("updateColorReader") JdbcPagingItemReader<ProductWorkGradePolicyGroup> reader,
                                @Qualifier("updateColorProcess") ItemProcessor<ProductWorkGradePolicyGroup, ProductWorkGradePolicyGroup> processor,
                                @Qualifier("updateColorWriter") JdbcBatchItemWriter<ProductWorkGradePolicyGroup> writer) {
        return new StepBuilder("updateColorStep", jobRepository)
                .<ProductWorkGradePolicyGroup, ProductWorkGradePolicyGroup>chunk(20, transactionManager)
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .transactionManager(transactionManager)
                .build();
    }

    @Bean
    @StepScope
    public JdbcPagingItemReader<ProductWorkGradePolicyGroup> updateColorReader(
            @Value("#{jobParameters['tenantId']}") String tenantId,
            @Value("#{jobParameters['colorId']}") String colorId,
            @Qualifier("defaultDataSource") DataSource dataSource) {

        JdbcPagingItemReader<ProductWorkGradePolicyGroup> reader = new JdbcPagingItemReader<>();
        reader.setDataSource(dataSource);
        reader.setPageSize(20);

        reader.setRowMapper((rs, rowNum) -> ProductWorkGradePolicyGroup.builder()
                .productWorkGradePolicyGroupId(rs.getLong("PRODUCT_WORK_GRADE_POLICY_GROUP_ID"))
                .build()
        );
        MySqlPagingQueryProvider queryProvider = new MySqlPagingQueryProvider();
        queryProvider.setSelectClause("SELECT *");
        queryProvider.setFromClause(tenantId + ".PRODUCT_WORK_GRADE_POLICY_GROUP ");
        queryProvider.setWhereClause("COLOR_ID = :colorId");
        queryProvider.setSortKeys(Map.of("PRODUCT_WORK_GRADE_POLICY_GROUP_ID", Order.ASCENDING));

        reader.setQueryProvider(queryProvider);

        Map<String, Object> parameterValues = new HashMap<>();
        parameterValues.put("colorId", colorId);
        reader.setParameterValues(parameterValues);

        return reader;
    }

    @Bean
    @StepScope
    public ItemProcessor<ProductWorkGradePolicyGroup, ProductWorkGradePolicyGroup> updateColorProcess() {
        return group -> {
            group.setColor(Color.builder().colorId(1L).build());
            return group;
        };
    }

    @Bean
    @StepScope
    public JdbcBatchItemWriter<ProductWorkGradePolicyGroup> updateColorWriter(
            @Value("#{jobParameters['tenantId']}") String tenantId,
            @Qualifier("defaultDataSource") DataSource dataSource) {

        JdbcBatchItemWriter<ProductWorkGradePolicyGroup> writer = new JdbcBatchItemWriter<>();
        writer.setDataSource(dataSource);
        writer.setSql("UPDATE " + tenantId + ".PRODUCT_WORK_GRADE_POLICY_GROUP SET COLOR_ID = :color.colorId WHERE PRODUCT_WORK_GRADE_POLICY_GROUP_ID = :productWorkGradePolicyGroupId");
        writer.setItemSqlParameterSourceProvider(new BeanPropertyItemSqlParameterSourceProvider<>());
        writer.afterPropertiesSet();
        return writer;
    }
}
