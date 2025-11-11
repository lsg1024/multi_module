package com.msa.account.global.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.msa.account.global.kafka.dto.GoldHarryDeletedEvent;
import com.msa.account.global.kafka.dto.GoldHarryLossUpdatedEvent;
import com.msa.account.global.kafka.dto.KafkaEventDto;
import com.msa.account.global.kafka.service.KafkaService;
import com.msa.common.global.exception.KafkaProcessingException;
import com.msa.common.global.redis.enum_type.RedisEventStatus;
import com.msa.common.global.redis.service.RedisEventService;
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
    private final RedisEventService redisEventService;

    public KafkaConsumer(ObjectMapper objectMapper, JobLauncher jobLauncher, Job updateStoreGoldHarryLossJob, Job deleteGoldHarryJob, KafkaService kafkaService, RedisEventService redisEventService) {
        this.objectMapper = objectMapper;
        this.jobLauncher = jobLauncher;
        this.updateStoreGoldHarryLossJob = updateStoreGoldHarryLossJob;
        this.deleteGoldHarryJob = deleteGoldHarryJob;
        this.kafkaService = kafkaService;
        this.redisEventService = redisEventService;
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
            RedisEventStatus status = redisEventService.checkAndSetProcessing(dto.getTenantId(), dto.getEventId());

            if (status == RedisEventStatus.COMPLETED || status == RedisEventStatus.PROCESSING) {
                log.warn("eventId check: 이미 실행되었거나 실행되는 중 eventId={}", dto.getEventId());
                ack.acknowledge();
                return;
            }
        } catch (Exception redisException) {
            log.error("Redis event check failed. db 내역 확인 필요: ", redisException);
        }

        try {
            TenantContext.setTenant(dto.getTenantId());
            AuditorHolder.setAuditor(dto.getTenantId());

            kafkaService.updateCurrentBalance(dto);

            // DB 처리 성공 -> Redis '완료' 상태로 변경
            redisEventService.setCompleted(dto.getTenantId(), dto.getEventId(), "COMPLETED");

            ack.acknowledge();

        } catch (DataIntegrityViolationException e) {
            log.warn("DB 제약 조건 오류, 동일한 값이 입력 eventId={}", dto.getEventId(), e);
            ack.acknowledge();

        } catch (Exception e) {
            log.error("알 수 없는 오류로 인해 재실행. eventId={}, err={}", dto.getEventId(), e.getMessage(), e);

            try {
                redisEventService.deleteEventId(dto.getTenantId(), dto.getEventId());
            } catch (Exception redisDelException) {
                log.error("Failed to delete 'PROCESSING' Redis 이벤트 삭제 실패", redisDelException);
            }

            throw new KafkaProcessingException("Kafka consume error, retrying");

        } finally {
            TenantContext.clear();
            AuditorHolder.clear();
        }
    }
}
