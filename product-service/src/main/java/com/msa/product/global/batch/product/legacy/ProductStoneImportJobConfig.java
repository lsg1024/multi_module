package com.msa.product.global.batch.product.legacy;

import com.msa.product.local.product.entity.ProductStone;
import com.msa.product.local.product.repository.ProductRepository;
import com.msa.product.local.product.repository.stone.ProductStoneRepository;
import com.msa.product.local.stone.stone.repository.StoneRepository;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.builder.JpaItemWriterBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.batch.item.file.transform.FieldSet;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * 레거시 모델별 스톤정보 CSV → ProductStone 마이그레이션 배치 Job 설정.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class ProductStoneImportJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final EntityManagerFactory entityManagerFactory;
    private final ProductRepository productRepository;
    private final StoneRepository stoneRepository;
    private final ProductStoneRepository productStoneRepository;
    private final StoneMigrationFailureCollector failureCollector;

    private static final int CHUNK_SIZE = 100;

    private static final String[] FIELD_NAMES = {
            "no", "modelNumber", "mainStone", "stoneName", "stoneNote",
            "includeQuantity", "stoneQuantity", "includeStone", "stoneWeight",
            "stonePurchasePrice", "includePrice", "gradePrice1", "gradePrice2",
            "gradePrice3", "gradePrice4"
    };

    // ── Job ──

    @Bean
    public Job productStoneLegacyImportJob() {
        return new JobBuilder("productStoneLegacyImportJob", jobRepository)
                .start(productStoneLegacyImportStep())
                .build();
    }

    @Bean
    public Step productStoneLegacyImportStep() {
        return new StepBuilder("productStoneLegacyImportStep", jobRepository)
                .<ProductStoneCsvRow, ProductStone>chunk(CHUNK_SIZE, transactionManager)
                .reader(productStoneCsvReader(null, null))
                .processor(productStoneCsvProcessor())
                .writer(productStoneJpaWriter())
                .build();
    }

    // ── Reader ──

    @Bean
    @StepScope
    public FlatFileItemReader<ProductStoneCsvRow> productStoneCsvReader(
            @Value("#{jobParameters['filePath']}") String filePath,
            @Value("#{jobParameters['encoding'] ?: 'UTF-8'}") String encoding) {

        DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer();
        tokenizer.setNames(FIELD_NAMES);
        tokenizer.setStrict(false);

        DefaultLineMapper<ProductStoneCsvRow> lineMapper = new DefaultLineMapper<>();
        lineMapper.setLineTokenizer(tokenizer);
        lineMapper.setFieldSetMapper(productStoneFieldSetMapper());

        return new FlatFileItemReaderBuilder<ProductStoneCsvRow>()
                .name("productStoneCsvReader")
                .resource(new FileSystemResource(filePath))
                .encoding(encoding)
                .linesToSkip(1)
                .lineMapper(lineMapper)
                .build();
    }

    // ── Processor ──

    @Bean
    @StepScope
    public ProductStoneCsvItemProcessor productStoneCsvProcessor() {
        return new ProductStoneCsvItemProcessor(productRepository, stoneRepository, productStoneRepository, failureCollector);
    }

    // ── Writer ──

    @Bean
    public JpaItemWriter<ProductStone> productStoneJpaWriter() {
        return new JpaItemWriterBuilder<ProductStone>()
                .entityManagerFactory(entityManagerFactory)
                .build();
    }

    // ── FieldSetMapper ──

    @Bean
    public FieldSetMapper<ProductStoneCsvRow> productStoneFieldSetMapper() {
        return (FieldSet fs) -> {
            ProductStoneCsvRow row = new ProductStoneCsvRow();
            row.setNo(fs.readString("no"));
            row.setModelNumber(fs.readString("modelNumber"));
            row.setMainStone(fs.readString("mainStone"));
            row.setStoneName(fs.readString("stoneName"));
            row.setStoneNote(fs.readString("stoneNote"));
            row.setIncludeQuantity(fs.readString("includeQuantity"));
            row.setStoneQuantity(fs.readString("stoneQuantity"));
            row.setIncludeStone(fs.readString("includeStone"));
            row.setStoneWeight(fs.readString("stoneWeight"));
            row.setStonePurchasePrice(fs.readString("stonePurchasePrice"));
            row.setIncludePrice(fs.readString("includePrice"));
            row.setGradePrice1(fs.readString("gradePrice1"));
            row.setGradePrice2(fs.readString("gradePrice2"));
            row.setGradePrice3(fs.readString("gradePrice3"));
            row.setGradePrice4(fs.readString("gradePrice4"));
            return row;
        };
    }
}
