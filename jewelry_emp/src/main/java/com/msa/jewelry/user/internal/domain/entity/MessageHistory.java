package com.msa.jewelry.user.internal.domain.entity;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "SMS 발송 이력 — 테넌트가 Naver SENS 로 발송한 모든 메시지의 성공/실패 기록을 보관한다.")
public class MessageHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    @Schema(description = "메시지 이력 PK", example = "100")
    private Long id;

    @Column(name = "TENANT_ID", nullable = false, length = 10)
    @Schema(description = "테넌트(고객사) 식별자", example = "tenant01")
    private String tenantId;

    @Column(name = "RECEIVER_PHONE", nullable = false, length = 15)
    @Schema(description = "수신자 전화번호 (최대 15자)", example = "01098765432")
    private String receiverPhone;

    @Column(name = "RECEIVER_NAME", length = 50)
    @Schema(description = "수신자 이름 (선택)", example = "홍길동")
    private String receiverName;

    @Column(name = "CONTENT", columnDefinition = "TEXT", nullable = false)
    @Schema(description = "발송 메시지 본문", example = "주문하신 상품이 출고되었습니다.")
    private String content;

    @Column(name = "STATUS", nullable = false, length = 20)
    @Schema(description = "발송 상태 (SUCCESS / FAIL 등)", example = "SUCCESS")
    private String status;

    @Column(name = "ERROR_MESSAGE", length = 500)
    @Schema(description = "발송 실패 시 에러 메시지", example = "Invalid phone number")
    private String errorMessage;

    @Column(name = "NAVER_REQUEST_ID", length = 100)
    @Schema(description = "Naver SENS API 응답으로 받은 요청 ID — SENS 콘솔에서 발송 추적용", example = "ncp-sens-req-abc123")
    private String naverRequestId;

    @Column(name = "SENT_BY", nullable = false, length = 50)
    @Schema(description = "발송 주체 (사용자 ID 또는 시스템명)", example = "admin01")
    private String sentBy;

    @Column(name = "CREATED_AT")
    @Schema(description = "발송 시각", example = "2026-05-16T14:30:00")
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
