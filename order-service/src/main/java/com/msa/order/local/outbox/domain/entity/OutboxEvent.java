package com.msa.order.local.outbox.domain.entity;

import com.msa.common.global.domain.BaseTimeEntity;
import com.msa.common.global.redis.enum_type.EventStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * Transactional Outbox 패턴 엔티티.
 *
 * *비즈니스 트랜잭션과 동일한 DB 트랜잭션 안에 저장되어,
 * 서비스 장애나 네트워크 오류 발생 시에도 이벤트 유실을 방지한다.
 * {@link OutboxRelayService}가 주기적으로 {@code PENDING} 상태의 레코드를 조회하여
 * Kafka로 발행한 뒤 {@code SENT}로 전이시킨다.
 *
 * *상태 전이: {@code PENDING} → {@code SENT} (성공) 또는 {@code FAILED} (최대 재시도 초과)
 */
@Getter
@Entity
@Table(name = "OUT_BOX_EVENT", indexes = {
        @Index(name = "idx_status_created", columnList = "EVENT_STATUS, created_at"),
        @Index(name = "idx_topic_status", columnList = "TOPIC, EVENT_STATUS")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OutboxEvent extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, name = "TOPIC")
    private String topic;

    @Column(nullable = false, name = "MESSAGE_KEY")
    private String messageKey;

    @Column(nullable = false, columnDefinition = "TEXT", name = "PAYLOAD")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "EVENT_STATUS")
    private EventStatus status = EventStatus.PENDING;

    @Column(name = "RETRY_COUNT")
    private int retryCount = 0;

    @Column(name = "LAST_ERROR_MESSAGE", length = 500)
    private String lastErrorMessage;

    /** 지수 백오프 적용 후 다음 재시도 가능 시각. {@code null}이면 즉시 재시도 가능. */
    @Column(name = "NEXT_RETRY_AT")
    private OffsetDateTime nextRetryAt;

    @Column(name = "EVENT_TYPE", length = 50)
    private String eventType;

    public OutboxEvent(String topic, String messageKey, String payload, String eventType) {
        this.topic = topic;
        this.messageKey = messageKey;
        this.payload = payload;
        this.eventType = eventType;
    }

    /**
     * 이벤트 상태를 {@code SENT}로 전이한다.
     * Kafka 전송 성공 직후 호출된다.
     */
    public void markAsSent() {
        this.status = EventStatus.SENT;
    }

    /**
     * 이벤트 상태를 {@code FAILED}로 전이하고 오류 메시지를 기록한다.
     * 재시도 없이 즉시 실패 처리할 때 사용한다.
     *
     * @param errorMessage 실패 원인 메시지
     */
    public void markAsFailed(String errorMessage) {
        this.status = EventStatus.FAILED;
        this.lastErrorMessage = errorMessage;
    }

    /**
     * 재시도 횟수를 1 증가시키고 지수 백오프 대기 시각을 설정한다.
     *
     * *대기 시간: {@code 2^(retryCount - 1)} 분 (1회→1분, 2회→2분, 3회→4분, 4회→8분, ...)
     * *재시도 횟수가 5회 이상이 되면 상태를 {@code FAILED}로 전이하여 더 이상 재시도하지 않는다.
     *
     * @param errorMessage 실패 원인 메시지
     */
    // 재시도 로직
    public void incrementRetryCount(String errorMessage) {
        this.retryCount++;
        this.lastErrorMessage = errorMessage;

        // 지수 백오프: 1분, 2분, 4분, 8분...
        long delayMinutes = (long) Math.pow(2, retryCount - 1);
        this.nextRetryAt = OffsetDateTime.now().plusMinutes(delayMinutes);

        if (this.retryCount >= 5) {
            this.status = EventStatus.FAILED;
        }
    }

    /**
     * 현재 이벤트가 재시도 가능한 상태인지 확인한다.
     *
     * *상태가 {@code PENDING}이고, {@code nextRetryAt}이 {@code null}이거나
     * 현재 시각이 {@code nextRetryAt}을 지난 경우에만 {@code true}를 반환한다.
     *
     * @return 재시도 가능하면 {@code true}
     */
    public boolean canRetry() {
        return this.status == EventStatus.PENDING
                && (this.nextRetryAt == null || OffsetDateTime.now().isAfter(this.nextRetryAt));
    }

    @Override
    public String toString() {
        return "OutboxEvent{" +
                "id=" + id +
                ", topic='" + topic + '\'' +
                ", messageKey='" + messageKey + '\'' +
                ", payload='" + payload + '\'' +
                ", status=" + status +
                ", retryCount=" + retryCount +
                ", lastErrorMessage='" + lastErrorMessage + '\'' +
                ", nextRetryAt=" + nextRetryAt +
                ", eventType='" + eventType + '\'' +
                '}';
    }
}