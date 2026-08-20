package com.msa.jewelry.local.order.controller;

import com.msa.common.global.api.ApiResponse;
import com.msa.common.global.tenant.TenantContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 레거시(khan) 주문 이관 endpoint.
 * 게이트웨이: POST /order/orders/migration (StripPrefix=1 → /orders/migration)
 * 입력: order_batch.json (tools/khan_orders_to_batch_json.py 산출물)
 */
@Slf4j
@RestController
public class OrderMigrationController {

    private final JobLauncher jobLauncher;
    private final Job orderMigrationJob;

    public OrderMigrationController(JobLauncher jobLauncher, Job orderMigrationJob) {
        this.jobLauncher = jobLauncher;
        this.orderMigrationJob = orderMigrationJob;
    }

    @PostMapping("/orders/migration")
    public ResponseEntity<ApiResponse<String>> migrateOrders(@RequestParam("file") MultipartFile file) {
        Path tempPath = null;
        try {
            tempPath = Files.createTempFile("order-migration-", ".json");
            file.transferTo(tempPath.toFile());

            JobParameters params = new JobParametersBuilder()
                    .addString("filePath", tempPath.toAbsolutePath().toString())
                    .addString("tenantId", TenantContext.getTenant())
                    .addLong("time", System.currentTimeMillis())
                    .toJobParameters();

            jobLauncher.run(orderMigrationJob, params);

        } catch (Exception e) {
            log.error("Order migration batch failed", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("주문 이관 실패: " + e.getMessage()));
        } finally {
            if (tempPath != null) {
                try { Files.deleteIfExists(tempPath); } catch (java.io.IOException ignored) {}
            }
        }
        return ResponseEntity.ok(ApiResponse.success("주문 이관 처리 중..."));
    }
}
