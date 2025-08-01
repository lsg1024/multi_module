package com.msa.account.global.batch;


import com.msa.account.global.domain.entity.CommonOption;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.JdbcPagingItemReader;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@Configuration
public class UpdateGoldHarryLossBatchJob {

    private final BatchCommonOptionUtil batchCommonOptionUtil;

    public UpdateGoldHarryLossBatchJob(BatchCommonOptionUtil batchCommonOptionUtil) {
        this.batchCommonOptionUtil = batchCommonOptionUtil;
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
                                        @Qualifier("updateCommonOptionReader") JdbcPagingItemReader<CommonOption> reader,
                                        @Qualifier("updateGoldHarryLossProcessor") ItemProcessor<CommonOption, CommonOption> processor,
                                        @Qualifier("updateCommonOptionWriter") JdbcBatchItemWriter<CommonOption> writer) {
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
    public JdbcPagingItemReader<CommonOption> updateCommonOptionReader(
            @Value("#{jobParameters['tenantId']}") String tenantId,
            @Value("#{jobParameters['goldHarryId']}") Long goldHarryId,
            @Qualifier("defaultDataSource") DataSource dataSource) {
        return batchCommonOptionUtil.createReader(tenantId, goldHarryId, dataSource);
    }

    @Bean
    @StepScope
    public ItemProcessor<CommonOption, CommonOption> updateGoldHarryLossProcessor(
            @Value("#{jobParameters['updatedGoldHarryLoss']}") String updatedGoldHarryLoss) {

        return commonOption -> {
            commonOption.updateGoldHarryLoss(updatedGoldHarryLoss);
            return commonOption;
        };
    }

    @Bean
    @StepScope
    public JdbcBatchItemWriter<CommonOption> updateCommonOptionWriter(
            @Value("#{jobParameters['tenantId']}") String tenantId,
            @Qualifier("defaultDataSource") DataSource dataSource) {
        JdbcBatchItemWriter<CommonOption> writer = new JdbcBatchItemWriter<>();
        writer.setDataSource(dataSource);

        //변경된 헤리 값을 수정
        String sql = "UPDATE " + tenantId + ".COMMON_OPTION SET GOLD_HARRY_LOSS = ? WHERE COMMON_OPTION_ID = ?";

        writer.setSql(sql);
        writer.setItemPreparedStatementSetter((item, ps) -> {
            ps.setString(1, item.getGoldHarryLoss());
            ps.setLong(2, item.getCommonOptionId());
        });

        return writer;
    }


}