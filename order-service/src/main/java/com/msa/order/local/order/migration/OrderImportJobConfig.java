package com.msa.order.local.order.migration;

import com.msa.order.global.feign_client.client.FactoryClient;
import com.msa.order.global.feign_client.client.ProductClient;
import com.msa.order.global.feign_client.client.StoreClient;
import com.msa.order.local.order.entity.Orders;
import com.msa.order.local.priority.repository.PriorityRepository;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemWriter;
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

import java.nio.charset.Charset;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class OrderImportJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final EntityManagerFactory entityManagerFactory;
    private final StoreClient storeClient;
    private final FactoryClient factoryClient;
    private final ProductClient productClient;
    private final MigrationFailureCollector failureCollector;
    private final PriorityRepository priorityRepository;

    private static final int CHUNK_SIZE = 500;
    private static final Charset CP949 = Charset.forName("CP949");
    private static final String[] FIELD_NAMES = {
            "no", "receiptNumber", "shopName", "category", "phase",
            "manufacturer", "manufacturingNo", "receiptDate", "shippingDate",
            "tradingPartner", "modelNumber", "classification", "quantity",
            "material", "color", "mainStone", "subStone", "size", "note"
    };

    // ── 일반 주문 마이그레이션 Job (CP949) ──

    @Bean
    public Job orderImportJob() {
        return new JobBuilder("orderImportJob", jobRepository)
                .start(orderImportStep())
                .build();
    }

    @Bean
    public Step orderImportStep() {
        return new StepBuilder("orderImportStep", jobRepository)
                .<OrderCsvRow, Orders>chunk(CHUNK_SIZE, transactionManager)
                .reader(orderCsvReader(null, null))
                .processor(orderCsvProcessor(null, null, null))
                .writer(orderMigrationWriter())
                .build();
    }

    // ── 삭재 주문 마이그레이션 Job ──

    @Bean
    public Job deletedOrderImportJob() {
        return new JobBuilder("deletedOrderImportJob", jobRepository)
                .start(deletedOrderImportStep())
                .build();
    }

    @Bean
    public Step deletedOrderImportStep() {
        return new StepBuilder("deletedOrderImportStep", jobRepository)
                .<OrderCsvRow, Orders>chunk(CHUNK_SIZE, transactionManager)
                .reader(orderCsvReader(null, null))
                .processor(orderCsvProcessor(null, null, null))
                .writer(orderMigrationWriter())
                .build();
    }

    // ── Reader / Processor ──

    @Bean
    @StepScope
    public FlatFileItemReader<OrderCsvRow> orderCsvReader(
            @Value("#{jobParameters['filePath']}") String filePath,
            @Value("#{jobParameters['encoding'] ?: 'CP949'}") String encoding) {

        DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer();
        tokenizer.setNames(FIELD_NAMES);
        tokenizer.setStrict(false); // 19열(원본) 또는 20열(실패CSV 재업로드) 모두 허용

        DefaultLineMapper<OrderCsvRow> lineMapper = new DefaultLineMapper<>();
        lineMapper.setLineTokenizer(tokenizer);
        lineMapper.setFieldSetMapper(orderCsvFieldSetMapper());

        return new FlatFileItemReaderBuilder<OrderCsvRow>()
                .name("orderCsvReader")
                .resource(new FileSystemResource(filePath))
                .encoding(encoding)
                .linesToSkip(1)
                .lineMapper(lineMapper)
                .build();
    }

    @Bean
    @StepScope
    public OrderCsvItemProcessor orderCsvProcessor(
            @Value("#{jobParameters['accessToken']}") String token,
            @Value("#{jobParameters['deleted'] ?: 'false'}") String deleted,
            @Value("#{jobParameters['fixOrder'] ?: 'false'}") String fixOrder) {
        boolean isDeleted = "true".equalsIgnoreCase(deleted);
        boolean isFixOrder = "true".equalsIgnoreCase(fixOrder);
        return new OrderCsvItemProcessor(token, storeClient, factoryClient, productClient, failureCollector, priorityRepository, isDeleted, isFixOrder);
    }

    /**
     * 마이그레이션 전용 Writer: Orders persist 후 StatusHistory 자동 생성.
     */
    @Bean
    public ItemWriter<Orders> orderMigrationWriter() {
        return new OrderMigrationItemWriter(entityManagerFactory);
    }

    @Bean
    public FieldSetMapper<OrderCsvRow> orderCsvFieldSetMapper() {
        return (FieldSet fieldSet) -> {
            OrderCsvRow row = new OrderCsvRow();
            row.setNo(fieldSet.readString("no"));
            row.setReceiptNumber(fieldSet.readString("receiptNumber"));
            row.setShopName(fieldSet.readString("shopName"));
            row.setCategory(fieldSet.readString("category"));
            row.setPhase(fieldSet.readString("phase"));
            row.setManufacturer(fieldSet.readString("manufacturer"));
            row.setManufacturingNo(fieldSet.readString("manufacturingNo"));
            row.setReceiptDate(fieldSet.readString("receiptDate"));
            row.setShippingDate(fieldSet.readString("shippingDate"));
            row.setTradingPartner(fieldSet.readString("tradingPartner"));
            row.setModelNumber(fieldSet.readString("modelNumber"));
            row.setClassification(fieldSet.readString("classification"));
            row.setQuantity(fieldSet.readString("quantity"));
            row.setMaterial(fieldSet.readString("material"));
            row.setColor(fieldSet.readString("color"));
            row.setMainStone(fieldSet.readString("mainStone"));
            row.setSubStone(fieldSet.readString("subStone"));
            row.setSize(fieldSet.readString("size"));
            row.setNote(fieldSet.readString("note"));
            return row;
        };
    }
}
