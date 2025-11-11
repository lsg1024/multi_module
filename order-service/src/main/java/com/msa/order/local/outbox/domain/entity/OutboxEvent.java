package com.msa.order.local.outbox.domain.entity;

import com.msa.common.global.domain.BaseTimeEntity;
import com.msa.common.global.redis.enum_type.EventStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "OUT_BOX_EVENT")
public class OutboxEvent extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, name = "TOPIC")
    private String topic;

    @Column(nullable = false, name = "MESSAGE_KEY")
    private String messageKey;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT", name = "PAYLOAD")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "EVENT_STATUS")
    private EventStatus status = EventStatus.PENDING;

    public OutboxEvent(String topic, String messageKey, String payload) {
        this.topic = topic;
        this.messageKey = messageKey;
        this.payload = payload;
    }

    public void markAsSent() {
        this.status = EventStatus.SENT;
    }

    public void markAsFailed() {
        this.status = EventStatus.FAILED;
    }

    @Override
    public String toString() {
        return "OutboxEvent{" +
                "id=" + id +
                ", topic='" + topic + '\'' +
                ", messageKey='" + messageKey + '\'' +
                ", payload='" + payload + '\'' +
                ", status=" + status +
                '}';
    }
}
