package com.msa.userserver.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "MESSAGE_HISTORY")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MessageHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "TENANT_ID", nullable = false, length = 10)
    private String tenantId;

    @Column(name = "RECEIVER_PHONE", nullable = false, length = 15)
    private String receiverPhone;

    @Column(name = "RECEIVER_NAME", length = 50)
    private String receiverName;

    @Column(name = "CONTENT", columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "STATUS", nullable = false, length = 20)
    private String status;

    @Column(name = "ERROR_MESSAGE", length = 500)
    private String errorMessage;

    @Column(name = "NAVER_REQUEST_ID", length = 100)
    private String naverRequestId;

    @Column(name = "SENT_BY", nullable = false, length = 50)
    private String sentBy;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @Builder
    public MessageHistory(String tenantId, String receiverPhone, String receiverName,
                          String content, String status, String errorMessage,
                          String naverRequestId, String sentBy) {
        this.tenantId = tenantId;
        this.receiverPhone = receiverPhone;
        this.receiverName = receiverName;
        this.content = content;
        this.status = status;
        this.errorMessage = errorMessage;
        this.naverRequestId = naverRequestId;
        this.sentBy = sentBy;
        this.createdAt = LocalDateTime.now();
    }
}
