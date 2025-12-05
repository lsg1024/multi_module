package com.msa.product.global.batch.product;

import com.msa.common.global.tenant.TenantContext;
import com.msa.product.local.product.entity.Product;
import com.msa.product.local.product.repository.ProductRepository;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.StringUtils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class ProductImageBatch {

    private final ProductRepository productRepository;

    @Value("${FILE_UPLOAD_PATH}")
    private String baseUploadPath;

    @Bean
    public Job imageMigrationJob(JobRepository jobRepository, Step imageMigrationStep) {
        return new JobBuilder("imageMigrationJob", jobRepository)
                .start(imageMigrationStep)
                .build();
    }

    @Bean
    @JobScope
    public Step imageMigrationStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            ItemReader<File> tempFileReader,
            ItemProcessor<File, ImageMoveDto> tempImageProcessor,
            ItemWriter<ImageMoveDto> tempImageWriter) {

        return new StepBuilder("imageMigrationStep", jobRepository)
                .<File, ImageMoveDto>chunk(100, transactionManager)
                .reader(tempFileReader)
                .processor(tempImageProcessor)
                .writer(tempImageWriter)
                .build();
    }

    // 1. Reader: temp 폴더의 파일 읽기
    @Bean
    @StepScope
    public ListItemReader<File> tempFileReader() {
        Path sourcePath = Paths.get(baseUploadPath, "temp");
        File sourceDir = sourcePath.toFile();

        if (!sourceDir.exists() || !sourceDir.isDirectory()) {
            log.error("Temp 디렉토리가 존재하지 않습니다: {}", sourcePath);
            return new ListItemReader<>(Collections.emptyList());
        }

        File[] files = sourceDir.listFiles(File::isFile);
        List<File> fileList = (files != null) ? Arrays.asList(files) : Collections.emptyList();

        log.info(">>>> [Batch] Temp 폴더에서 {}개의 파일을 발견했습니다.", fileList.size());
        return new ListItemReader<>(fileList);
    }

    // 2. Processor: 파일명 -> ProductId 매핑
    @Bean
    @StepScope
    public ItemProcessor<File, ImageMoveDto> tempImageProcessor() {
        return new ItemProcessor<File, ImageMoveDto>() {

            private Map<String, Long> productCache;
            private String tenant;

            @BeforeStep
            public void beforeStep(StepExecution stepExecution) {
                tenant = TenantContext.getTenant();
                if (!StringUtils.hasText(this.tenant)) {
                    throw new IllegalArgumentException("Tenant 정보가 없습니다.");
                }

                log.info(">>>> [Batch] 상품 데이터 캐싱 시작 (Name -> ID)");
                try {
                    productCache = productRepository.findAll().stream()
                            .collect(Collectors.toMap(
                                    Product::getProductName,
                                    Product::getProductId,
                                    (oldVal, newVal) -> oldVal
                            ));
                    log.info(">>>> [Batch] 캐싱 완료. 상품 수: {}", productCache.size());
                } finally {
                    TenantContext.clear();
                }

            }

            @Override
            public ImageMoveDto process(File file) {
                String fileName = file.getName();
                int dotIndex = fileName.lastIndexOf('.');
                String productName = (dotIndex == -1) ? fileName : fileName.substring(0, dotIndex);
                String extension = (dotIndex == -1) ? ".jpg" : fileName.substring(dotIndex);

                Long productId = productCache.get(productName.trim());

                if (productId == null) {
                    log.warn("매칭되는 상품 없음 (Skip): {}", fileName);
                    return null;
                }

                return new ImageMoveDto(file, productId, extension, tenant);
            }
        };
    }

    // 3. Writer: 폴더 생성 및 이동 (Rename)
    @Bean
    @StepScope
    public ItemWriter<ImageMoveDto> tempImageWriter() {
        return items -> {
            for (ImageMoveDto item : items) {
                try {
                    Path productDir = Paths.get(baseUploadPath, item.getTenant(), "products", String.valueOf(item.getProductId()));

                    if (!Files.exists(productDir)) {
                        Files.createDirectories(productDir);
                    }

                    long fileCount;
                    try (var stream = Files.list(productDir)) {
                        fileCount = stream.count();
                    }
                    String newFileName = (fileCount + 1) + item.getExtension();
                    Path targetPath = productDir.resolve(newFileName);

                    Thumbnails.of(item.getFile())
                            .size(300, 300)
                            .outputQuality(1)
                            .toFile(targetPath.toFile());

                    if (Files.exists(targetPath)) {
                        Files.delete(item.getFile().toPath());
                        log.info("가공 이동 완료: {} -> {} (Size Optimized)", item.getFile().getName(), targetPath);
                    }

                } catch (Exception e) {
                    log.error("이미지 처리 실패: " + item.getFile().getName(), e);
                }
            }
        };
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ImageMoveDto {
        private File file;
        private Long productId;
        private String extension;
        private String tenant;
    }
}