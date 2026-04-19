package com.msa.account.global.batch.saleLog;

import com.msa.account.global.batch.TenantAwareJobListener;
import com.msa.account.local.transaction_history.domain.entity.SaleLog;
import jakarta.persistence.EntityManagerFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaItemWriterBuilder;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Map;

/**
 * SaleLog 잔액 재계산 배치 Job 설정.
 *
 * *과거 또는 중간 시점에 거래가 삽입되었을 때, 그 이후의 모든 {@link SaleLog} 레코드의
 * before/after 잔액을 시간순으로 재계산하여 running balance의 정합성을 복원한다.
 *
 * *처리 흐름:
 *
 *   - <b>Reader</b>({@code rebalanceReader}): 대상 {@code storeId}의 모든 {@link SaleLog}를
 *       {@code saleDate ASC, id ASC} 순서로 JPA 페이징 조회한다.
 *   - <b>Processor</b>({@code rebalanceProcessor}): {@link SaleLogStatefulProcessor}를 사용하여
 *       상태를 유지하며 각 로그의 잔액을 누적 재계산한다.
 *   - <b>Writer</b>({@code rebalanceWriter}): 재계산된 {@link SaleLog}를 JPA merge로 저장한다.
 * 
 *
 * *청크 크기: {@value #CHUNK_SIZE}건.
 * {@link TenantAwareJobListener}를 통해 멀티테넌트 컨텍스트를 Job 실행 전후로 설정/해제한다.
 *
 * *트리거: {@code KafkaConsumer.handleUpdateCurrentBalance()} 에서 중간 삽입이 감지된 경우.
 */
@Slf4j
@Configuration
public class SaleLogUpdateBatchConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final EntityManagerFactory entityManagerFactory;
    private final TenantAwareJobListener tenantAwareJobListener;
    private static final int CHUNK_SIZE = 500;

    public SaleLogUpdateBatchConfig(JobRepository jobRepository, PlatformTransactionManager transactionManager, EntityManagerFactory entityManagerFactory, TenantAwareJobListener tenantAwareJobListener) {
        this.jobRepository = jobRepository;
        this.transactionManager = transactionManager;
        this.entityManagerFactory = entityManagerFactory;
        this.tenantAwareJobListener = tenantAwareJobListener;
    }

    @Bean
    public Job saleLogRebalanceJob() {
        return new JobBuilder("saleLogUpdateJob", jobRepository)
                .listener(tenantAwareJobListener)
                .start(saleLogUpdateStep())
                .build();
    }

    @Bean
    public Step saleLogUpdateStep() {
        return new StepBuilder("saleLogUpdateStep", jobRepository)
                .<SaleLog, SaleLog>chunk(CHUNK_SIZE, transactionManager)
                .reader(rebalanceReader(null))
                .processor(rebalanceProcessor(null))
                .writer(rebalanceWriter())
                .build();
    }

    @Bean
    @StepScope
    public JpaPagingItemReader<SaleLog> rebalanceReader(
            @Value("#{jobParameters['storeId']}") Long storeId) {

        return new JpaPagingItemReaderBuilder<SaleLog>()
                .name("saleLogReader")
                .entityManagerFactory(entityManagerFactory)
                .pageSize(CHUNK_SIZE)
                .queryString("SELECT s FROM SaleLog s WHERE s.store.storeId = :storeId ORDER BY s.saleDate ASC, s.id ASC")
                .parameterValues(Map.of("storeId", storeId))
                .build();
    }

    @Bean
    @StepScope
    public ItemProcessor<SaleLog, SaleLog> rebalanceProcessor(@Value("#{jobParameters['storeId']}") Long storeId) {
        return new SaleLogStatefulProcessor();
    }

    @Bean
    public JpaItemWriter<SaleLog> rebalanceWriter() {
        return new JpaItemWriterBuilder<SaleLog>()
                .entityManagerFactory(entityManagerFactory)
                .build();
    }

}
