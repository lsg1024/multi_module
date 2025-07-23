package com.msa.account.global.batch;


import com.msa.account.global.domain.entity.CommonOption;
import com.msa.account.global.domain.entity.GoldHarry;
import com.msa.account.global.domain.repository.GoldHarryRepository;
import com.msa.account.global.exception.NotFoundException;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
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
public class UpdateGoldHarryLossBatchJob {

    private final GoldHarryRepository goldHarryRepository;

    public UpdateGoldHarryLossBatchJob(GoldHarryRepository goldHarryRepository) {
        this.goldHarryRepository = goldHarryRepository;
    }

    @Bean
    public Job updateStoreGoldHarryLossJob(JobRepository jobRepository, Step updateGoldHarryLossStep) {
        return new JobBuilder("updateGoldHarryLossJob", jobRepository)
                .start(updateGoldHarryLossStep)
                .build();
    }

    @Bean
    public Step updateGoldHarryLossStep(JobRepository jobRepository,
                                        PlatformTransactionManager transactionManager,
                                        JdbcPagingItemReader<CommonOption> reader,
                                        ItemProcessor<CommonOption, CommonOption> processor,
                                        JdbcBatchItemWriter<CommonOption> writer) {
        return new StepBuilder("updateGoldHarryLossStep", jobRepository)
                .<CommonOption, CommonOption>chunk(100, transactionManager)
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .transactionManager(transactionManager)
                .build();
    }

    @Bean
    @StepScope
    public JdbcPagingItemReader<CommonOption> commonOptionReader(
            @Value("#{jobParameters['tenantId']}") String tenantId,
            @Value("#{jobParameters['goldHarryId']}") Long goldHarryId,
            @Qualifier("defaultDataSource") DataSource dataSource) {
        JdbcPagingItemReader<CommonOption> reader = new JdbcPagingItemReader<>();
        reader.setDataSource(dataSource);
        reader.setPageSize(100);

        reader.setRowMapper((rs, rowNum) -> {
            GoldHarry goldHarry = GoldHarry.builder()
                    .goldHarryId(rs.getLong("GOLD_HARRY_ID"))
                    .build();

            return CommonOption.builder()
                    .commonOptionId(rs.getString("COMMON_OPTION_ID"))
                    .goldHarryLoss(rs.getString("GOLD_HARRY_LOSS"))
                    .goldHarry(goldHarry)
                    .build();
        });

        MySqlPagingQueryProvider provider = new MySqlPagingQueryProvider();
        provider.setSelectClause("SELECT co.*");
        provider.setFromClause(tenantId + ".COMMON_OPTION co " +
                "JOIN " + tenantId + ".GOLD_HARRY gh ON gh.GOLD_HARRY_ID = co.GOLD_HARRY_ID");
        provider.setWhereClause("gh.GOLD_HARRY_ID = :goldHarryId");

        Map<String, Order> sortKeys = new HashMap<>();
        sortKeys.put("COMMON_OPTION_ID", Order.ASCENDING);
        provider.setSortKeys(sortKeys);

        reader.setQueryProvider(provider);

        // 파라미터
        Map<String, Object> parameterValues = new HashMap<>();
        parameterValues.put("goldHarryId", goldHarryId);
        reader.setParameterValues(parameterValues);

        return reader;
    }

    @Bean
    @StepScope
    public ItemProcessor<CommonOption, CommonOption> updateGoldHarryLossProcessor(
            @Value("#{jobParameters['goldHarryId']}") Long goldHarryId,
            @Value("#{jobParameters['updatedGoldHarryLoss']}") String updatedGoldHarryLoss) {


        return option -> {
            if (!option.getGoldHarry().getGoldHarryId().equals(goldHarryId)) {
                GoldHarry newGoldHarry = goldHarryRepository.findById(goldHarryId)
                        .orElseThrow(() -> new NotFoundException("GoldHarry not found"));

                option.updateGoldHarry(newGoldHarry);
            } else {
                option.updateGoldHarryLoss(updatedGoldHarryLoss);
            }

            return option;
        };
    }

    @Bean
    @StepScope
    public JdbcBatchItemWriter<CommonOption> commonOptionWriter(
            @Value("#{jobParameters['tenantId']}") String tenantId,
            @Qualifier("defaultDataSource") DataSource dataSource) {
        JdbcBatchItemWriter<CommonOption> writer = new JdbcBatchItemWriter<>();
        writer.setDataSource(dataSource);

        // 스키마명을 동적으로 조립
        String sql = "UPDATE " + tenantId + ".COMMON_OPTION SET GOLD_HARRY_LOSS = ? WHERE COMMON_OPTION_ID = ?";

        writer.setSql(sql);
        writer.setItemPreparedStatementSetter((item, ps) -> {
            ps.setString(1, item.getGoldHarryLoss());
            ps.setLong(2, item.getCommonOptionId());
        });

        return writer;
    }


}