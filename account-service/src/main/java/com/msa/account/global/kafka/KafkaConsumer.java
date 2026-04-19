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

/**
 * Kafka 컨슈머 — 3개 토픽을 구독하여 배치 잡 또는 잔액 갱신 서비스를 호출한다.
 *
 * *구독 토픽 및 처리 흐름:
 *
 *   - <b>goldHarryLoss.update</b>: 해리(손모율) 수정 이벤트 수신 →
 *       {@code UpdateGoldHarryLossBatchJob} 실행하여 연관 CommonOption의 goldHarryLoss 일괄 갱신
 *   - <b>goldHarry.deleted</b>: 해리 삭제 이벤트 수신 →
 *       {@code DeleteGoldHarryBatchJob} 실행하여 해당 CommonOption을 기본 해리(ID=1)로 대체
 *   - <b>current-balance-update</b>: 매장/공장 잔액 변동 이벤트 수신 →
 *       {@link KafkaService#updateCurrentBalance} 호출 후 중간 삽입 감지 시
 *       {@code SaleLogRebalanceJob}을 실행하여 이후 잔액을 재계산
 * 
 *
 * *의존 컴포넌트: {@link KafkaService}, {@link SaleLogRepository},
 * {@link JobLauncher}, Spring Batch Job 빈 3개
 */
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

    /**
     * {@code goldHarryLoss.update} 토픽 메시지를 수신하여 해리 손모율 일괄 갱신 배치를 실행한다.
     *
     * *이벤트에서 {@code tenantId}, {@code goldHarryId}, {@code newGoldHarryLoss}를 추출하고
     * {@code UpdateGoldHarryLossBatchJob}에 JobParameters로 전달한다.
     * 파싱 또는 배치 실행 실패 시 {@link IllegalArgumentException}으로 래핑하여 재처리를 막는다.
     *
     * @param message Kafka로부터 수신된 JSON 문자열 ({@link GoldHarryLossUpdatedEvent})
     */
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

    /**
     * {@code goldHarry.deleted} 토픽 메시지를 수신하여 해리 삭제 후속 처리 배치를 실행한다.
     *
     * *삭제된 해리를 참조하는 모든 {@link com.msa.account.global.domain.entity.CommonOption}을
     * 기본 해리(ID=1)로 대체하는 {@code DeleteGoldHarryBatchJob}을 실행한다.
     * 파싱 또는 배치 실행 실패 시 {@link IllegalArgumentException}으로 래핑된다.
     *
     * @param message Kafka로부터 수신된 JSON 문자열 ({@link GoldHarryDeletedEvent})
     */
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

    /**
     * {@code current-balance-update} 토픽 메시지를 수신하여 Store/Factory 잔액을 갱신한다.
     *
     * *처리 흐름:
     *
     *   - JSON을 {@link KafkaEventDto.updateCurrentBalance}로 파싱한다. 파싱 실패 시
     *       메시지를 건너뛰고(ack) 로그만 남긴다.
     *   - {@link KafkaService#updateCurrentBalance}를 호출하여 잔액을 갱신하고
     *       새 {@link SaleLog}를 저장한다.
     *   - 저장된 {@link SaleLog}보다 이후 시각의 로그가 존재하는지 확인하여
     *       <b>과거/중간 삽입</b>이 감지되면 {@code SaleLogRebalanceJob}을 실행한다.
     *   - {@link org.springframework.dao.DataIntegrityViolationException} 발생 시
     *       중복 이벤트로 간주하고 ack 처리 후 조용히 종료한다.
     *   - 그 외 예외는 {@link com.msa.common.global.exception.KafkaProcessingException}으로
     *       래핑하여 {@code @RetryableTopic} 재시도 대상이 되도록 한다.
     * 
     *
     * @param message Kafka로부터 수신된 JSON 문자열
     * @param ack     수동 오프셋 커밋용 {@link Acknowledgment}
     */
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

        } catch (IllegalArgumentException e) {
            // 멱등성 체크: 이미 처리된 이벤트이거나 잘못된 파라미터 → 재시도 불필요
            log.warn("이벤트 처리 스킵 (중복 또는 잘못된 요청). eventId={}, reason={}", dto.getEventId(), e.getMessage());
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
