package com.msa.jewelry.global.batch.order;

import com.msa.jewelry.global.batch.TenantAwareJobListener;
import com.msa.jewelry.local.order.entity.Orders;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.json.JacksonJsonObjectReader;
import org.springframework.batch.item.json.JsonItemReader;
import org.springframework.batch.item.json.builder.JsonItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * 레거시(khan) 주문 이관 Batch.
 * order_batch.json → Orders(+OrderProduct+OrderStone)(+Stock for STOCK).
 * 멱등(legacyOrderNo) · master 자동생성 · 멀티테넌시(TenantAwareJobListener) 보장.
 */
@Configuration
@RequiredArgsConstructor
public class OrderMigrationBatchConfig {

    private static final int CHUNK_SIZE = 200;

    private final OrderMigrationProcessor orderMigrationProcessor;
    private final OrderMigrationWriter orderMigrationWriter;
    private final TenantAwareJobListener tenantAwareJobListener;

    @Bean
    public Job orderMigrationJob(JobRepository jobRepository, Step orderMigrationStep) {
        return new JobBuilder("orderMigrationJob", jobRepository)
                .listener(tenantAwareJobListener)
                .start(orderMigrationStep)
                .build();
    }

    @Bean
    @JobScope
    public Step orderMigrationStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            ItemReader<OrderBatchDto> orderJsonReader) {
        return new StepBuilder("orderMigrationStep", jobRepository)
                .<OrderBatchDto, Orders>chunk(CHUNK_SIZE, transactionManager)
                .reader(orderJsonReader)
                .processor(orderMigrationProcessor)
                .listener(orderMigrationProcessor)   // @BeforeStep (캐시 로드) 활성화
                .writer(orderMigrationWriter)
                .build();
    }

    @Bean
    @StepScope
    public JsonItemReader<OrderBatchDto> orderJsonReader(
            @Value("#{jobParameters['filePath']}") String filePath) {
        return new JsonItemReaderBuilder<OrderBatchDto>()
                .name("orderJsonReader")
                .jsonObjectReader(new JacksonJsonObjectReader<>(OrderBatchDto.class))
                .resource(new FileSystemResource(filePath))
                .build();
    }
}
