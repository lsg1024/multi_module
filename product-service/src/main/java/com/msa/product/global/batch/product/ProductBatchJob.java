package com.msa.product.global.batch.product;

import com.msa.product.local.product.dto.ProductBatchDto;
import com.msa.product.local.product.entity.Product;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.builder.JpaItemWriterBuilder;
import org.springframework.batch.item.json.JacksonJsonObjectReader;
import org.springframework.batch.item.json.JsonItemReader;
import org.springframework.batch.item.json.builder.JsonItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;

@Component
@RequiredArgsConstructor
public class ProductBatchJob {

    private final EntityManagerFactory entityManagerFactory;
    private final ProductItemProcessor productItemProcessor;

    private static final int CHUNK_SIZE = 1000;

    @Bean
    public Job productInsertJob(JobRepository jobRepository, Step productInsertStep) {
        return new JobBuilder("productInsertJob", jobRepository)
                .start(productInsertStep)
                .build();
    }
    @Bean
    @JobScope
    public Step productInsertStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            ItemReader<ProductBatchDto> productJsonReader,
            ItemWriter<Product> productItemWriter) {

        return new StepBuilder("productInsertStep", jobRepository)
                .<ProductBatchDto, Product>chunk(CHUNK_SIZE, transactionManager)
                .reader(productJsonReader)
                .processor(productItemProcessor) // ★ 여기에 로직 없이 Bean만 연결!
                .writer(productItemWriter)
                .build();
    }

    @Bean
    @StepScope
    public JsonItemReader<ProductBatchDto> productJsonReader(
            @Value("#{jobParameters['filePath']}") String filePath) {

        return new JsonItemReaderBuilder<ProductBatchDto>()
                .name("productJsonReader")
                .jsonObjectReader(new JacksonJsonObjectReader<>(ProductBatchDto.class))
                .resource(new FileSystemResource(filePath))
                .build();
    }

    @Bean
    @StepScope
    public JpaItemWriter<Product> productItemWriter() {
        return new JpaItemWriterBuilder<Product>()
                .entityManagerFactory(entityManagerFactory)
                .build();
    }

}
