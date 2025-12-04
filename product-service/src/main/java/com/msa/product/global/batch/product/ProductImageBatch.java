package com.msa.product.global.batch.product;

import com.msa.product.local.product.entity.Product;
import com.msa.product.local.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import java.nio.file.StandardCopyOption;
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
        // 컨테이너 내부 경로: /app/images/temp
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
                this.tenant = stepExecution.getJobParameters().getString("tenant");
                if (!StringUtils.hasText(this.tenant)) {
                    throw new IllegalArgumentException("Tenant 정보가 없습니다.");
                }

                log.info(">>>> [Batch] 상품 데이터 캐싱 시작 (Name -> ID)");
                productCache = productRepository.findAll().stream()
                        .collect(Collectors.toMap(
                                Product::getProductName,
                                Product::getProductId,
                                (oldVal, newVal) -> oldVal
                        ));
                log.info(">>>> [Batch] 캐싱 완료. 상품 수: {}", productCache.size());
            }

            @Override
            public ImageMoveDto process(File file) {
                String fileName = file.getName();
                int dotIndex = fileName.lastIndexOf('.');
                String productName = (dotIndex == -1) ? fileName : fileName.substring(0, dotIndex);
                String extension = (dotIndex == -1) ? ".jpg" : fileName.substring(dotIndex);

                // 파일명(상품명) 공백 제거 등 전처리 필요시 추가
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
                    Path productDir = Paths.get(baseUploadPath, item.getTenant(), String.valueOf(item.getProductId()));

                    if (!Files.exists(productDir)) {
                        Files.createDirectories(productDir);
                    }

                    long fileCount;
                    try (var stream = Files.list(productDir)) {
                        fileCount = stream.count();
                    }

                    String newFileName = (fileCount + 1) + item.getExtension();
                    Path targetFile = productDir.resolve(newFileName);

                    Files.move(item.getFile().toPath(), targetFile, StandardCopyOption.REPLACE_EXISTING);

                    log.info("이동 완료: {} -> {}/{}", item.getFile().getName(), item.getProductId(), newFileName);

                } catch (Exception e) {
                    log.error("파일 이동 실패: " + item.getFile().getName(), e);
                }
            }
        };
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class ImageMoveDto {
        private File file;
        private Long productId;
        private String extension;
        private String tenant;
    }
}