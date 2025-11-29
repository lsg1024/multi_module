package com.msa.account.global.batch;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.msa.account.global.domain.entity.GoldHarry;
import com.msa.account.global.domain.repository.GoldHarryRepository;
import com.msa.account.local.factory.domain.dto.FactoryDto;
import com.msa.account.local.factory.domain.entity.Factory;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope; // [필수]
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
public class CreateFactoryBatchJob {
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final EntityManagerFactory entityManagerFactory;
    private final GoldHarryRepository goldHarryRepository;

    private static final int CHUNK_SIZE = 1000;

    @Bean
    public Job factoryImportJob() {
        return new JobBuilder("factoryImportJob", jobRepository)
                .start(factoryImportStep())
                .build();
    }

    @Bean
    public Step factoryImportStep() {
        return new StepBuilder("factoryImportStep", jobRepository)
                .<FactoryDto.FactoryRequest, Factory>chunk(CHUNK_SIZE, transactionManager)
                .reader(factoryJsonReader(null))
                .processor(factoryItemProcessor())
                .writer(factoryJpaWriter())
                .build();
    }

    @Bean
    @StepScope
    public JsonItemReader<FactoryDto.FactoryRequest> factoryJsonReader(
            @Value("#{jobParameters['filePath']}") String filePath) {

        ObjectMapper objectMapper = new ObjectMapper();
        JacksonJsonObjectReader<FactoryDto.FactoryRequest> jsonObjectReader =
                new JacksonJsonObjectReader<>(FactoryDto.FactoryRequest.class);
        jsonObjectReader.setMapper(objectMapper);

        return new JsonItemReaderBuilder<FactoryDto.FactoryRequest>()
                .jsonObjectReader(jsonObjectReader)
                .resource(new FileSystemResource(filePath))
                .name("factoryJsonReader")
                .build();
    }

    @Bean
    public ItemProcessor<FactoryDto.FactoryRequest, Factory> factoryItemProcessor() {
        return request -> {
            String goldHarryId = request.getCommonOptionInfo().getGoldHarryId();

            GoldHarry goldHarry = goldHarryRepository.findById(Long.valueOf(goldHarryId))
                    .orElseThrow(() -> new IllegalArgumentException("Invalid GoldHarry ID: " + goldHarryId));

            return request.toEntity(goldHarry);
        };
    }

    @Bean
    public JpaItemWriter<Factory> factoryJpaWriter() {
        return new JpaItemWriterBuilder<Factory>()
                .entityManagerFactory(entityManagerFactory)
                .build();
    }
}