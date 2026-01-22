package com.msa.order.local.outbox.domain.entity;

import com.msa.common.global.domain.BaseTimeEntity;
import com.msa.common.global.redis.enum_type.EventStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

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

    public void markAsSent() {
        this.status = EventStatus.SENT;
    }

    public void markAsFailed(String errorMessage) {
        this.status = EventStatus.FAILED;
        this.lastErrorMessage = errorMessage;
    }

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