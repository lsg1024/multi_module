package com.msa.account.global.batch;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.msa.account.global.domain.entity.GoldHarry;
import com.msa.account.global.domain.repository.GoldHarryRepository;
import com.msa.account.local.store.domain.dto.StoreDto;
import com.msa.account.local.store.domain.entity.Store;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.builder.JpaItemWriterBuilder;
import org.springframework.batch.item.json.JacksonJsonObjectReader;
import org.springframework.batch.item.json.JsonItemReader;
import org.springframework.batch.item.json.builder.JsonItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.transaction.PlatformTransactionManager;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class CreateStoreBatchJob {
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final EntityManagerFactory entityManagerFactory;
    private final GoldHarryRepository goldHarryRepository;

    private static final int CHUNK_SIZE = 1000;

    @Bean
    public Job storeImportJob() {
        return new JobBuilder("storeImportJob", jobRepository)
                .start(storeImportStep())
                .build();
    }

    @Bean
    public Step storeImportStep() {
        return new StepBuilder("storeImportStep", jobRepository)
                .<StoreDto.StoreRequest, Store>chunk(CHUNK_SIZE, transactionManager)
                .reader(storeJsonReader(null))
                .processor(storeItemProcessor())
                .writer(storeJpaWriter())
                .build();
    }

    @Bean
    @StepScope
    public JsonItemReader<StoreDto.StoreRequest> storeJsonReader(
            @Value("#{jobParameters['filePath']}") String filePath) {

        ObjectMapper objectMapper = new ObjectMapper();
        JacksonJsonObjectReader<StoreDto.StoreRequest> jsonObjectReader =
                new JacksonJsonObjectReader<>(StoreDto.StoreRequest.class);
        jsonObjectReader.setMapper(objectMapper);

        return new JsonItemReaderBuilder<StoreDto.StoreRequest>()
                .jsonObjectReader(jsonObjectReader)
                .resource(new FileSystemResource(filePath))
                .name("storeJsonReader")
                .build();
    }

    @Bean
    public ItemProcessor<StoreDto.StoreRequest, Store> storeItemProcessor() {
        return request -> {
            String goldHarryId = request.getCommonOptionInfo().getGoldHarryId();

            GoldHarry goldHarry = goldHarryRepository.findById(Long.valueOf(goldHarryId))
                    .orElseThrow(() -> new IllegalArgumentException("Invalid GoldHarry ID: " + goldHarryId));

            return request.toEntity(goldHarry);
        };
    }

    @Bean
    public JpaItemWriter<Store> storeJpaWriter() {
        return new JpaItemWriterBuilder<Store>()
                .entityManagerFactory(entityManagerFactory)
                .build();
    }
}