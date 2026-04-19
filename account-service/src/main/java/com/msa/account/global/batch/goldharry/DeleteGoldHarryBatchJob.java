package com.msa.account.global.batch.goldharry;

import com.msa.account.global.batch.goldharry.BatchCommonOptionUtil;
import com.msa.account.global.domain.entity.CommonOption;
import com.msa.account.global.domain.entity.GoldHarry;
import com.msa.account.global.domain.repository.GoldHarryRepository;
import com.msa.account.global.exception.ExceptionMessage;
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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

/**
 * 해리 삭제 시 연관 CommonOption을 기본 해리(ID=1)로 대체하는 Spring Batch Job 설정.
 *
 * *{@code goldHarry.deleted} Kafka 토픽 이벤트를 수신한 {@code KafkaConsumer}가
 * 이 Job을 실행한다. 처리 흐름:
 *
 *   - <b>Reader</b>({@code deleteCommonOptionReader}): 삭제된 {@code goldHarryId}를 참조하는
 *       모든 {@link CommonOption}을 JDBC 페이징으로 읽는다.
 *   - <b>Processor</b>({@code deleteGoldHarryDefaultProcessor}): 시스템 기본 해리(ID=1)를
 *       조회하여 각 {@link CommonOption}의 {@code goldHarry} 참조와 {@code goldHarryLoss} 사본을
 *       기본값으로 교체한다.
 *   - <b>Writer</b>({@code deleteCommonOptionWriter}): 변경된 {@code GOLD_HARRY_ID} 및
 *       {@code GOLD_HARRY_LOSS}를 JDBC 배치 UPDATE로 반영한다.
 * 
 *
 * *청크 크기: 100건. 기본 해리(ID=1)가 존재하지 않으면 {@link NotFoundException}이 발생한다.
 * {@code @StepScope} 어노테이션은 애플리케이션 구동 시 스키마 미보유 오류를 방지하기 위해 사용된다.
 */
@Configuration
public class DeleteGoldHarryBatchJob {

    private final BatchCommonOptionUtil batchCommonOptionUtil;
    private final GoldHarryRepository goldHarryRepository;

    public DeleteGoldHarryBatchJob(BatchCommonOptionUtil batchCommonOptionUtil, GoldHarryRepository goldHarryRepository) {
        this.batchCommonOptionUtil = batchCommonOptionUtil;
        this.goldHarryRepository = goldHarryRepository;
    }

    @Bean
    public Job deleteGoldHarryJob(JobRepository jobRepository, Step deleteGoldHarryStep) {
        return new JobBuilder("deleteGoldHarryJob", jobRepository)
                .start(deleteGoldHarryStep)
                .build();
    }

    @Bean
    public Step deleteGoldHarryStep(JobRepository jobRepository,
                                    PlatformTransactionManager transactionManager,
                                    @Qualifier("deleteCommonOptionReader") JdbcPagingItemReader<CommonOption> reader,
                                    @Qualifier("deleteGoldHarryDefaultProcessor") ItemProcessor<CommonOption, CommonOption> processor,
                                    @Qualifier("deleteCommonOptionWriter") JdbcBatchItemWriter<CommonOption> writer) {

        return new StepBuilder("deleteGoldHarryStep", jobRepository)
                .<CommonOption, CommonOption>chunk(100, transactionManager)
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .transactionManager(transactionManager)
                .build();
    }

    @Bean
    @StepScope
    public JdbcPagingItemReader<CommonOption> deleteCommonOptionReader(
            @Value("#{jobParameters['tenantId']}") String tenantId,
            @Value("#{jobParameters['goldHarryId']}") Long goldHarryId,
            @Qualifier("defaultDataSource") DataSource dataSource) {
        return batchCommonOptionUtil.createReader(tenantId, goldHarryId, dataSource);
    }
    @Bean
    @StepScope //구동 시 빈 등록 시 스키마 미보유 오류 발생
    public ItemProcessor<CommonOption, CommonOption> deleteGoldHarryDefaultProcessor() {
        GoldHarry defaultGoldHarry = goldHarryRepository.findById(1L)
                .orElseThrow(() -> new NotFoundException(ExceptionMessage.NOT_FOUND));

        return commonOption -> {
            commonOption.updateGoldHarry(defaultGoldHarry);
            return commonOption;
        };
    }

    @Bean
    @StepScope
    public JdbcBatchItemWriter<CommonOption> deleteCommonOptionWriter(
            @Value("#{jobParameters['tenantId']}") String tenantId,
            @Qualifier("defaultDataSource") DataSource dataSource) {
        JdbcBatchItemWriter<CommonOption> writer = new JdbcBatchItemWriter<>();
        writer.setDataSource(dataSource);

        String sql = "UPDATE " + tenantId + ".COMMON_OPTION SET GOLD_HARRY_ID = ?, GOLD_HARRY_LOSS = ? WHERE COMMON_OPTION_ID = ?";

        writer.setSql(sql);
        writer.setItemPreparedStatementSetter((item, ps) -> {
            ps.setLong(1, 1L);
            ps.setString(2, item.getGoldHarryLoss());
            ps.setLong(3, item.getCommonOptionId());
        });

        return writer;
    }
}
