package com.msa.account.global.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.msa.account.global.kafka.dto.GoldHarryDeletedEvent;
import com.msa.account.global.kafka.dto.GoldHarryLossUpdatedEvent;
import com.msa.account.global.kafka.dto.KafkaEventDto;
import com.msa.account.global.kafka.service.KafkaService;
import com.msa.account.local.transaction_history.domain.entity.SaleLog;
import com.msa.account.local.transaction_history.repository.SaleLogRepository;
import com.msa.common.global.exception.KafkaProcessingException;
import com.msa.common.global.tenant.TenantContext;
import com.msa.common.global.util.AuditorHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class KafkaConsumer {

    private final ObjectMapper objectMapper;
    private final JobLauncher jobLauncher;
    private final Job updateStoreGoldHarryLossJob;
    private final Job deleteGoldHarryJob;
    private final Job saleLogRebalanceJob;
    private final KafkaService kafkaService;
    private final SaleLogRepository saleLogRepository;

    public KafkaConsumer(ObjectMapper objectMapper, JobLauncher jobLauncher, Job updateStoreGoldHarryLossJob, Job deleteGoldHarryJob, Job saleLogRebalanceJob, KafkaService kafkaService, SaleLogRepository saleLogRepository) {
        this.objectMapper = objectMapper;
        this.jobLauncher = jobLauncher;
        this.updateStoreGoldHarryLossJob = updateStoreGoldHarryLossJob;
        this.deleteGoldHarryJob = deleteGoldHarryJob;
        this.saleLogRebalanceJob = saleLogRebalanceJob;
        this.kafkaService = kafkaService;
        this.saleLogRepository = saleLogRepository;
    }
    @KafkaListener(topics = "goldHarryLoss.update", groupId = "goldHarry-group", concurrency = "3")
    public void handleGoldHarryLossUpdate(String message) {
        try {
            GoldHarryLossUpdatedEvent event = objectMapper.readValue(message, GoldHarryLossUpdatedEvent.class);

            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("tenantId", event.tenantId())
                    .addLong("goldHarryId", event.goldHarryId())
                    .addString("updatedGoldHarryLoss", event.newGoldHarryLoss())
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();
            jobLauncher.run(updateStoreGoldHarryLossJob, jobParameters);
        } catch (Exception e) {
            log.error("Parse or batch launch failed", e);
            throw new IllegalArgumentException(e);
        }
    }

    @KafkaListener(topics = "goldHarry.deleted", groupId = "goldHarry-group", concurrency = "3")
    public void handleGoldHarryDelete(String message) {
        try {
            GoldHarryDeletedEvent event = objectMapper.readValue(message, GoldHarryDeletedEvent.class);

            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("tenantId", event.tenantId())
                    .addString("goldHarryId", event.goldHarryId())
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();

            jobLauncher.run(deleteGoldHarryJob, jobParameters);
        } catch (Exception e) {
            log.error("Parse or batch launch failed", e);
            throw new IllegalArgumentException(e);
        }
    }

    @KafkaListener(topics = "current-balance-update", groupId = "currentBalance-group", concurrency = "3")
    @RetryableTopic(attempts = "2", backoff = @Backoff(delay = 1000, maxDelay = 5000, random = true), include = KafkaProcessingException.class)
    public void handleUpdateCurrentBalance(String message, Acknowledgment ack) {
        KafkaEventDto.updateCurrentBalance dto;

        try {
            dto = objectMapper.readValue(message, KafkaEventDto.updateCurrentBalance.class);
        } catch (Exception e) {
            log.error("JSON parse failed, message skipped. payload={}", message, e);
            ack.acknowledge();
            return;
        }

        try {
            TenantContext.setTenant(dto.getTenantId());
            AuditorHolder.setAuditor(dto.getTenantId());

            SaleLog savedLog = kafkaService.updateCurrentBalance(dto);

            boolean isInsertedInMiddle = saleLogRepository.existsFutureLog(
                    dto.getId(),
                    savedLog.getSaleDate(),
                    savedLog.getId()
            );

            if (isInsertedInMiddle) {
                log.info("[Rebalance Triggered] 과거/중간 삽입 감지. StoreId={}, Date={}, ID={}",
                        dto.getId(), savedLog.getSaleDate(), savedLog.getId());

                JobParameters jobParameters = new JobParametersBuilder()
                        .addString("tenantId", dto.getTenantId())
                        .addLong("storeId", dto.getId())
                        .addLong("triggerLogId", savedLog.getId())
                        .addLong("timestamp", System.currentTimeMillis())
                        .toJobParameters();

                jobLauncher.run(saleLogRebalanceJob, jobParameters);
            } else {
                log.info("[Skip Rebalance] 최신 거래 등록. StoreId={}, ID={}", dto.getId(), savedLog.getId());
            }

            ack.acknowledge();

        } catch (DataIntegrityViolationException e) {
            log.warn("DB 제약 조건 오류, 동일한 값이 입력 eventId={}", dto.getEventId(), e);
            ack.acknowledge();

        } catch (Exception e) {
            log.error("알 수 없는 오류로 인해 재실행. eventId={}, err={}", dto.getEventId(), e.getMessage(), e);

            throw new KafkaProcessingException("Kafka consume error, retrying");

        } finally {
            TenantContext.clear();
            AuditorHolder.clear();
        }
    }
}
