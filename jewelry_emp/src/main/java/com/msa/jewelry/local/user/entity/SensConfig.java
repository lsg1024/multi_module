package com.msa.jewelry.local.user.entity;

import com.msa.common.global.domain.BaseTimeEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "SENS_CONFIG")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Schema(description = "테넌트별 Naver Cloud Platform SENS(SMS) API 자격증명. 테넌트마다 독립된 NCP 계정/발신번호를 사용할 수 있도록 tenantId UNIQUE 키로 저장한다.")
public class SensConfig extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    @Schema(description = "SENS 설정 PK", example = "1")
    private Long id;

    @Column(name = "TENANT_ID", unique = true, nullable = false, length = 10)
    @Schema(description = "테넌트(고객사) 식별자 — UNIQUE, 테넌트당 1건만 존재", example = "tenant01")
    private String tenantId;

    @Column(name = "ACCESS_KEY", nullable = false)
    @Schema(description = "NCP IAM 액세스 키 (민감 정보 — 응답 노출 주의)", example = "ncp_iam_BPAMKR********")
    private String accessKey;

    @Column(name = "SECRET_KEY", nullable = false)
    @Schema(description = "HMAC-SHA256 서명 생성용 시크릿 키 (민감 정보 — 응답 노출 금지)", example = "secret_********************")
    private String secretKey;

    @Column(name = "SERVICE_ID", nullable = false)
    @Schema(description = "Naver SENS SMS 서비스 ID", example = "ncp:sms:kr:123456789012:my-service")
    private String serviceId;

    @Column(name = "SENDER_PHONE", nullable = false, length = 15)
    @Schema(description = "SMS 발신자 전화번호 (사전 등록 필요, 최대 15자)", example = "01012345678")
    private String senderPhone;

    @Column(name = "ENABLED", nullable = false)
    @Schema(description = "SENS 서비스 활성화 여부 — false 면 SMS 발송 차단", example = "true")
    private boolean enabled = true;

    @Builder
    public SensConfig(String tenantId, String accessKey, String secretKey, String serviceId, String senderPhone) {
        this.tenantId = tenantId;
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.serviceId = serviceId;
        this.senderPhone = senderPhone;
    }

    public void update(String accessKey, String secretKey, String serviceId, String senderPhone) {
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.serviceId = serviceId;
        this.senderPhone = senderPhone;
    }

    public void updateEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
