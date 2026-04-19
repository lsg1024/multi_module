package com.msa.userserver.domain.entity;

import com.msa.common.global.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 테넌트별 Naver SENS API 자격증명 엔티티.
 *
 * *각 테넌트가 독립적인 Naver Cloud Platform SENS 계정을 사용할 수 있도록
 * tenantId를 UNIQUE 키로 하여 자격증명을 저장한다.
 *
 * *저장 필드:
 *
 *   - {@code tenantId} — 테넌트 식별자 (UNIQUE, 최대 10자)
 *   - {@code accessKey} — NCP IAM 액세스 키
 *   - {@code secretKey} — HMAC-SHA256 서명 생성에 사용하는 시크릿 키
 *   - {@code serviceId} — SENS SMS 서비스 ID
 *   - {@code senderPhone} — 발신자 전화번호 (최대 15자)
 *   - {@code enabled} — SENS 서비스 활성화 여부 (기본값 {@code true})
 * 
 *
 * *의존성: {@link com.msa.common.global.domain.BaseTimeEntity} (생성·수정 시각 자동 관리)
 */
@Getter
@Entity
@Table(name = "SENS_CONFIG")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SensConfig extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "TENANT_ID", unique = true, nullable = false, length = 10)
    private String tenantId;

    @Column(name = "ACCESS_KEY", nullable = false)
    private String accessKey;

    @Column(name = "SECRET_KEY", nullable = false)
    private String secretKey;

    @Column(name = "SERVICE_ID", nullable = false)
    private String serviceId;

    @Column(name = "SENDER_PHONE", nullable = false, length = 15)
    private String senderPhone;

    @Column(name = "ENABLED", nullable = false)
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
