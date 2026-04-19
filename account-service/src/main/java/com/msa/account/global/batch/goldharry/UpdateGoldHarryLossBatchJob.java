package com.msa.account.global.batch.goldharry;


import com.msa.account.global.batch.goldharry.BatchCommonOptionUtil;
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

/**
 * 해리 손모율 변경 시 연관 CommonOption을 일괄 갱신하는 Spring Batch Job 설정.
 *
 * *{@code goldHarryLoss.update} Kafka 토픽 이벤트를 수신한 {@code KafkaConsumer}가
 * 이 Job을 실행한다. 처리 흐름:
 *
 *   - <b>Reader</b>({@code updateCommonOptionReader}): 변경된 {@code goldHarryId}를 참조하는
 *       모든 {@link CommonOption}을 JDBC 페이징으로 읽는다.
 *   - <b>Processor</b>({@code updateGoldHarryLossProcessor}): 각 {@link CommonOption}의
 *       {@code goldHarryLoss} 사본 값을 새 손모율 문자열로 교체한다.
 *   - <b>Writer</b>({@code updateCommonOptionWriter}): 변경된 값을 JDBC 배치 UPDATE로
 *       해당 테넌트 스키마에 반영한다.
 * 
 *
 * *청크 크기: 100건. 멀티테넌트 환경에서 {@code tenantId}를 JobParameter로 받아
 * 스키마를 동적으로 지정한다.
 */
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