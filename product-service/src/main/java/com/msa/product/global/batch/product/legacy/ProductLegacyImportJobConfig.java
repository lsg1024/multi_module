package com.msa.product.global.batch.product.legacy;

import com.msa.product.global.feign_client.client.FactoryClient;
import com.msa.product.local.classification.repository.ClassificationRepository;
import com.msa.product.local.color.repository.ColorRepository;
import com.msa.product.local.material.repository.MaterialRepository;
import com.msa.product.local.product.dto.FactoryDto;
import com.msa.product.local.product.entity.Product;
import com.msa.product.local.product.repository.ProductRepository;
import com.msa.product.local.set.repository.SetTypeRepository;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.builder.JpaItemWriterBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.batch.item.file.transform.FieldSet;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 레거시 기본정보 CSV → Product 마이그레이션 배치 Job 설정.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class ProductLegacyImportJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final EntityManagerFactory entityManagerFactory;
    private final ProductRepository productRepository;
    private final SetTypeRepository setTypeRepository;
    private final ClassificationRepository classificationRepository;
    private final MaterialRepository materialRepository;
    private final ColorRepository colorRepository;
    private final FactoryClient factoryClient;
    private final ProductMigrationFailureCollector failureCollector;

    private static final int CHUNK_SIZE = 100;

    private static final String[] FIELD_NAMES = {
            "no", "registerDate", "modelNumber", "manufacturer", "manufacturingNo",
            "setType", "classification", "material", "standardWeight", "isPublic",
            "discontinued", "unitPrice", "note", "defaultColor", "purchasePrice",
            "grade1LaborCost", "grade2LaborCost", "grade3LaborCost", "grade4LaborCost",
            "laborCostNote"
    };

    // ── Job ──

    @Bean
    public Job productLegacyImportJob() {
        return new JobBuilder("productLegacyImportJob", jobRepository)
                .start(productLegacyImportStep())
                .build();
    }

    @Bean
    public Step productLegacyImportStep() {
        return new StepBuilder("productLegacyImportStep", jobRepository)
                .<ProductLegacyCsvRow, Product>chunk(CHUNK_SIZE, transactionManager)
                .reader(productLegacyReversedReader(null, null))
                .processor(productLegacyCsvProcessor(null))
                .writer(productLegacyJpaWriter())
                .build();
    }

    // ── Reader (CSV 전체 읽기 → 역순 반전) ──

    /**
     * CSV가 신규→구형 순이므로, 구형→신규 순으로 뒤집어서 저장한다.
     * FlatFileItemReader로 전체 행을 읽은 뒤 Collections.reverse()로 반전.
     */
    @Bean
    @StepScope
    public ItemReader<ProductLegacyCsvRow> productLegacyReversedReader(
            @Value("#{jobParameters['filePath']}") String filePath,
            @Value("#{jobParameters['encoding'] ?: 'UTF-8'}") String encoding) {

        FlatFileItemReader<ProductLegacyCsvRow> delegate = buildCsvReader(filePath, encoding);

        try {
            delegate.open(new org.springframework.batch.item.ExecutionContext());
            List<ProductLegacyCsvRow> allRows = new ArrayList<>();
            ProductLegacyCsvRow row;
            while ((row = delegate.read()) != null) {
                allRows.add(row);
            }
            delegate.close();

            Collections.reverse(allRows);
            log.info("[레거시 상품 배치] CSV {}건 읽기 완료 → 역순 반전", allRows.size());

            return new ListItemReader<>(allRows);
        } catch (Exception e) {
            throw new RuntimeException("CSV 읽기 실패", e);
        }
    }

    private FlatFileItemReader<ProductLegacyCsvRow> buildCsvReader(String filePath, String encoding) {
        DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer();
        tokenizer.setNames(FIELD_NAMES);
        tokenizer.setStrict(false);

        DefaultLineMapper<ProductLegacyCsvRow> lineMapper = new DefaultLineMapper<>();
        lineMapper.setLineTokenizer(tokenizer);
        lineMapper.setFieldSetMapper(productLegacyFieldSetMapper());

        return new FlatFileItemReaderBuilder<ProductLegacyCsvRow>()
                .name("productLegacyCsvReader")
                .resource(new FileSystemResource(filePath))
                .encoding(encoding)
                .linesToSkip(1)
                .lineMapper(lineMapper)
                .build();
    }

    // ── Processor ──

    @Bean
    @StepScope
    public ProductLegacyCsvItemProcessor productLegacyCsvProcessor(
            @Value("#{jobParameters['accessToken']}") String accessToken) {

        // Factory 캐시 구성 (기존 ProductItemProcessor와 동일한 패턴)
        Map<String, FactoryDto.ResponseBatch> factoryCache = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        try {
            List<FactoryDto.ResponseBatch> factories = factoryClient.getFactories(accessToken);
            for (FactoryDto.ResponseBatch f : factories) {
                factoryCache.putIfAbsent(f.getFactoryName(), f);
            }
            log.info("[레거시 상품 배치] 공장 캐시 로드 완료: {}건", factoryCache.size());
        } catch (Exception e) {
            log.error("[레거시 상품 배치] 공장 정보 로드 실패 — 모든 상품이 '제조사 없음'으로 실패합니다!", e);
            // 빈 캐시로 진행하면 모든 행이 실패하므로 경고 로그 강화
        }

        return new ProductLegacyCsvItemProcessor(
                productRepository, setTypeRepository, classificationRepository,
                materialRepository, colorRepository, failureCollector, factoryCache
        );
    }

    // ── Writer ──

    @Bean
    public JpaItemWriter<Product> productLegacyJpaWriter() {
        return new JpaItemWriterBuilder<Product>()
                .entityManagerFactory(entityManagerFactory)
                .build();
    }

    // ── FieldSetMapper ──

    @Bean
    public FieldSetMapper<ProductLegacyCsvRow> productLegacyFieldSetMapper() {
        return (FieldSet fs) -> {
            ProductLegacyCsvRow row = new ProductLegacyCsvRow();
            row.setNo(fs.readString("no"));
            row.setRegisterDate(fs.readString("registerDate"));
            row.setModelNumber(fs.readString("modelNumber"));
            row.setManufacturer(fs.readString("manufacturer"));
            row.setManufacturingNo(fs.readString("manufacturingNo"));
            row.setSetType(fs.readString("setType"));
            row.setClassification(fs.readString("classification"));
            row.setMaterial(fs.readString("material"));
            row.setStandardWeight(fs.readString("standardWeight"));
            row.setIsPublic(fs.readString("isPublic"));
            row.setDiscontinued(fs.readString("discontinued"));
            row.setUnitPrice(fs.readString("unitPrice"));
            row.setNote(fs.readString("note"));
            row.setDefaultColor(fs.readString("defaultColor"));
            row.setPurchasePrice(fs.readString("purchasePrice"));
            row.setGrade1LaborCost(fs.readString("grade1LaborCost"));
            row.setGrade2LaborCost(fs.readString("grade2LaborCost"));
            row.setGrade3LaborCost(fs.readString("grade3LaborCost"));
            row.setGrade4LaborCost(fs.readString("grade4LaborCost"));
            row.setLaborCostNote(fs.readString("laborCostNote"));
            return row;
        };
    }
}
