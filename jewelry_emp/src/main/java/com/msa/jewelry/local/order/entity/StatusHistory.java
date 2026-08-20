package com.msa.jewelry.local.order.entity;

import com.msa.jewelry.local.order.dto.StatusHistoryDto;
import com.msa.jewelry.local.order.entity.order_enum.BusinessPhase;
import com.msa.jewelry.local.order.entity.order_enum.Kind;
import com.msa.jewelry.local.order.entity.order_enum.SourceType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Getter
@Table(name = "STATUS_HISTORY", indexes = {
        @Index(name = "idx_hist_flowcode_created", columnList = "FLOW_CODE, CREATED_AT")
})
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Schema(description = "상태 변경 이력(불변 감사 로그) — 주문/재고/판매의 비즈니스 상태 전이를 flowCode 기준 시계열로 기록.")
public class StatusHistory {
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    @Schema(description = "이력 PK", example = "9001")
    private Long id;

    @Column(name = "FLOW_CODE", nullable = false)
    @Schema(description = "TSID 기반 전역 흐름 코드 — 주문/재고/판매 공통 추적 키", example = "445823472384938240")
    private Long flowCode;

    @Enumerated(EnumType.STRING) @Column(name = "SOURCE_TYPE")
    @Schema(description = "이력 출처 유형 (ORDER/STOCK/SALE 등)", example = "ORDER")
    private SourceType sourceType;

    @Enumerated(EnumType.STRING) @Column(name = "PHASE")
    @Schema(description = "전이된 비즈니스 단계 (UPDATE/FAIL/WAITING 등)", example = "WAITING")
    private BusinessPhase phase;

    @Enumerated(EnumType.STRING) @Column(name = "KIND")
    @Schema(description = "변경 종류 (CREATE/UPDATE/DELETE)", example = "UPDATE")
    private Kind kind;
    @Column(name = "FROM_VALUE")
    @Schema(description = "전이 이전 상태 이름", example = "WAITING")
    private String fromValue;
    @Column(name = "TO_VALUE")
    @Schema(description = "전이 이후 상태 이름", example = "STOCK")
    private String toValue;
    @Column(name = "CONTENT", columnDefinition = "TEXT")
    @Schema(description = "이력 상세 내용/메모", example = "재고 입고 처리 완료")
    private String content;
    /** 상태 변경 이력 발생 시각 (도메인 컬럼) — KST 기준. */
    @Column(name = "CREATED_AT")
    @Schema(description = "이력 발생 시각 (KST)", example = "2026-05-16T14:30:00")
    private LocalDateTime createAt;
    @Column(name = "USER_NAME")
    @Schema(description = "이력을 발생시킨 사용자 이름", example = "홍길동")
    private String userName;

    @PrePersist
    void prePersist() {
        if (createAt == null) createAt = LocalDateTime.now(KST);
    }

    public static StatusHistory create(Long flowCode, SourceType src,
                                       BusinessPhase to, Kind kind, String userName,
                                       String content) {
        StatusHistory h = new StatusHistory();
        h.flowCode = flowCode;
        h.sourceType = src;
        h.phase = to;
        h.fromValue = null;
        h.toValue = to.name();
        h.kind = kind;
        h.userName = userName;
        h.content = content;
        return h;
    }
    public static StatusHistory phaseChange(Long flowCode, SourceType src,
                                            BusinessPhase from, BusinessPhase to, String content, String userName) {
        StatusHistory h = base(flowCode, src, Kind.UPDATE, content, userName);
        h.phase = to;
        h.fromValue = from != null ? from.name() : null;
        h.toValue = to.name();
        return h;
    }

    protected static StatusHistory base(Long flow, SourceType src, Kind kind, String content, String userName) {
        StatusHistory h = new StatusHistory();
        h.flowCode = flow; h.sourceType = src; h.kind = kind;
        h.content = content;
        h.userName = userName;
        return h;
    }

    public StatusHistoryDto toDto() {
        return new StatusHistoryDto(
                this.phase != null ? this.phase.getDisplayName() : null,
                this.kind != null ? this.kind.getDisplayName() : null,
                this.content,
                this.createAt,
                this.userName
        );
    }

    /**
     * 새로운 flowCode로 StatusHistory를 복사합니다.
     * 삭제 시 분기점을 만들기 위해 사용됩니다.
     * @param newFlowCode 새로운 flowCode
     * @return 복사된 StatusHistory (저장되지 않은 상태)
     */
    public StatusHistory copyWithNewFlowCode(Long newFlowCode) {
        StatusHistory copy = new StatusHistory();
        copy.flowCode = newFlowCode;
        copy.sourceType = this.sourceType;
        copy.phase = this.phase;
        copy.kind = this.kind;
        copy.fromValue = this.fromValue;
        copy.toValue = this.toValue;
        copy.content = this.content;
        copy.createAt = this.createAt;
        copy.userName = this.userName;
        return copy;
    }
}
