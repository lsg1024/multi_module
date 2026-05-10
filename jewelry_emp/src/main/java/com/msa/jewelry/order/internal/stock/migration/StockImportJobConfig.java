package com.msa.jewelry.order.internal.stock.migration;

import com.msa.jewelry.order.internal.global.feign_legacy.client.FactoryClient;
import com.msa.jewelry.order.internal.global.feign_legacy.client.ProductClient;
import com.msa.jewelry.order.internal.global.feign_legacy.client.StoreClient;
import com.msa.jewelry.order.internal.stock.entity.Stock;
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
import org.springframework.batch.item.file.FlatFileParseException;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
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
public class StockImportJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final EntityManagerFactory entityManagerFactory;
    private final StoreClient storeClient;
    private final FactoryClient factoryClient;
    private final ProductClient productClient;
    private final StockMigrationFailureCollector failureCollector;
    private final StockMigrationRecordCollector recordCollector;

    private static final int CHUNK_SIZE = 50;
    private static final Charset CP949 = Charset.forName("CP949");
    private static final String[] FIELD_NAMES = {
            "no", "storeName", "storeGrade", "sourceType", "currentStockType",
            "createdDate", "changedDate", "serialNumber", "receiptNumber", "modelName",
            "classification", "material", "color", "mainStone", "subStone",
            "size", "stockNote", "unitProduct", "totalWeight", "goldWeight",
            "stoneWeight", "quantity", "mainStoneQuantity", "subStoneQuantity", "laborCostFixed",
            "productLaborCost", "productAddLaborCost", "stoneMainLaborCost", "stoneSubLaborCost", "totalLaborCost",
            "factoryName", "factoryHarry", "productPurchaseCost", "totalStonePurchaseCost"
    };

    @Bean
    public Job stockImportJob() {
        return new JobBuilder("stockImportJob", jobRepository)
                .start(stockImportStep())
                .build();
    }

    @Bean
    public Step stockImportStep() {
        return new StepBuilder("stockImportStep", jobRepository)
                .<StockCsvRow, Stock>chunk(CHUNK_SIZE, transactionManager)
                .reader(stockCsvReader(null, null))
                .processor(stockCsvProcessor(null))
                .writer(stockMigrationWriter(null))
                .faultTolerant()
                .skip(FlatFileParseException.class)   // Reader CSV 파싱 오류 skip
                .skip(Exception.class)                 // Processor/Writer 예외도 skip
                .skipLimit(Integer.MAX_VALUE)           // skip 무제한 (실패는 failureCollector에 기록)
                .listener(new StockMigrationSkipListener(failureCollector))
                .build();
    }

    @Bean
    @StepScope
    public FlatFileItemReader<StockCsvRow> stockCsvReader(
            @Value("#{jobParameters['filePath']}") String filePath,
            @Value("#{jobParameters['encoding'] ?: 'UTF-8'}") String encoding) {

        DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer();
        tokenizer.setNames(FIELD_NAMES);
        tokenizer.setStrict(false); // 34열 허용

        DefaultLineMapper<StockCsvRow> lineMapper = new DefaultLineMapper<>();
        lineMapper.setLineTokenizer(tokenizer);
        lineMapper.setFieldSetMapper(this::stockCsvFieldSetMapper);

        return new FlatFileItemReaderBuilder<StockCsvRow>()
                .name("stockCsvReader")
                .resource(new FileSystemResource(filePath))
                .encoding(encoding)
                .linesToSkip(1)
                .lineMapper(lineMapper)
                .build();
    }

    /**
     * FieldSet → StockCsvRow 매핑
     */
    private StockCsvRow stockCsvFieldSetMapper(FieldSet fieldSet) {
        StockCsvRow row = new StockCsvRow();
        row.setNo(fieldSet.readString("no"));
        row.setStoreName(fieldSet.readString("storeName"));
        row.setStoreGrade(fieldSet.readString("storeGrade"));
        row.setSourceType(fieldSet.readString("sourceType"));
        row.setCurrentStockType(fieldSet.readString("currentStockType"));
        row.setCreatedDate(fieldSet.readString("createdDate"));
        row.setChangedDate(fieldSet.readString("changedDate"));
        row.setSerialNumber(fieldSet.readString("serialNumber"));
        row.setReceiptNumber(fieldSet.readString("receiptNumber"));
        row.setModelName(fieldSet.readString("modelName"));
        row.setClassification(fieldSet.readString("classification"));
        row.setMaterial(fieldSet.readString("material"));
        row.setColor(fieldSet.readString("color"));
        row.setMainStone(fieldSet.readString("mainStone"));
        row.setSubStone(fieldSet.readString("subStone"));
        row.setSize(fieldSet.readString("size"));
        row.setStockNote(fieldSet.readString("stockNote"));
        row.setUnitProduct(fieldSet.readString("unitProduct"));
        row.setTotalWeight(fieldSet.readString("totalWeight"));
        row.setGoldWeight(fieldSet.readString("goldWeight"));
        row.setStoneWeight(fieldSet.readString("stoneWeight"));
        row.setQuantity(fieldSet.readString("quantity"));
        row.setMainStoneQuantity(fieldSet.readString("mainStoneQuantity"));
        row.setSubStoneQuantity(fieldSet.readString("subStoneQuantity"));
        row.setLaborCostFixed(fieldSet.readString("laborCostFixed"));
        row.setProductLaborCost(fieldSet.readString("productLaborCost"));
        row.setProductAddLaborCost(fieldSet.readString("productAddLaborCost"));
        row.setStoneMainLaborCost(fieldSet.readString("stoneMainLaborCost"));
        row.setStoneSubLaborCost(fieldSet.readString("stoneSubLaborCost"));
        row.setTotalLaborCost(fieldSet.readString("totalLaborCost"));
        row.setFactoryName(fieldSet.readString("factoryName"));
        row.setFactoryHarry(fieldSet.readString("factoryHarry"));
        row.setProductPurchaseCost(fieldSet.readString("productPurchaseCost"));
        row.setTotalStonePurchaseCost(fieldSet.readString("totalStonePurchaseCost"));
        return row;
    }

    @Bean
    @StepScope
    public StockCsvItemProcessor stockCsvProcessor(
            @Value("#{jobParameters['accessToken']}") String token) {
        return new StockCsvItemProcessor(token, storeClient, factoryClient, productClient,
                failureCollector, recordCollector);
    }

    /**
     * 마이그레이션 전용 Writer: Stock persist 후 StatusHistory 자동 생성.
     *
     * <p>JobParameter "userName" 을 통해 StatusHistory.userName 값을 주입한다.
     * 미지정 시 기존 동작과 호환되도록 "LEGACY_MIGRATION" 으로 기본 설정된다.
     * 크롤링 등 별도 출처 데이터는 호출 측에서 다른 값(예: "CRAWL_MIGRATION") 을 지정하여
     * StatusHistory 기준으로 추후 식별/분류가 가능하다.</p>
     */
    @Bean
    @StepScope
    public ItemWriter<Stock> stockMigrationWriter(
            @Value("#{jobParameters['userName'] ?: 'LEGACY_MIGRATION'}") String userName) {
        return new StockMigrationItemWriter(entityManagerFactory, failureCollector, recordCollector, userName);
    }
}
