package com.msa.account.global.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.msa.account.global.kafka.dto.GoldHarryDeletedEvent;
import com.msa.account.global.kafka.dto.GoldHarryLossUpdatedEvent;
import com.msa.account.global.kafka.dto.KafkaEventDto;
import com.msa.account.global.kafka.service.KafkaService;
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
    private final KafkaService kafkaService;

    public KafkaConsumer(ObjectMapper objectMapper, JobLauncher jobLauncher, Job updateStoreGoldHarryLossJob, Job deleteGoldHarryJob, KafkaService kafkaService) {
        this.objectMapper = objectMapper;
        this.jobLauncher = jobLauncher;
        this.updateStoreGoldHarryLossJob = updateStoreGoldHarryLossJob;
        this.deleteGoldHarryJob = deleteGoldHarryJob;
        this.kafkaService = kafkaService;
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

    @KafkaListener(topics = "update.currentBalance", groupId = "currentBalance-group", concurrency = "3")
    @RetryableTopic(attempts = "2", backoff = @Backoff(delay = 1000, maxDelay = 5000, random = true), include = KafkaProcessingException.class)
    public void handleUpdateCurrentBalance(String message, Acknowledgment ack) {
        try {
            KafkaEventDto.updateCurrentBalance updateCurrentBalance = objectMapper.readValue(message, KafkaEventDto.updateCurrentBalance.class);
            TenantContext.setTenant(updateCurrentBalance.getTenantId());
            AuditorHolder.setAuditor(updateCurrentBalance.getTenantId());

            kafkaService.updateCurrentBalance(updateCurrentBalance);

            ack.acknowledge();
        } catch (DataIntegrityViolationException e) {
            log.warn("이미 처리된 eventId = {}", e.getMessage());
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Consume failed. payload={}, err={}", message, e.getMessage(), e);
            throw new IllegalStateException("Kafka consume error", e);
        } finally {
            AuditorHolder.clear();
        }
    }
}
