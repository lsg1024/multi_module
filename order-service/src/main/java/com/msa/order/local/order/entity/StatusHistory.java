package com.msa.order.local.order.entity;

import com.msa.order.global.dto.StatusHistoryDto;
import com.msa.order.local.order.entity.order_enum.BusinessPhase;
import com.msa.order.local.order.entity.order_enum.Kind;
import com.msa.order.local.order.entity.order_enum.SourceType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.time.ZoneId;

/**
 * 상태 변경 이력 감사 추적 엔티티.
 *
 * *주문·재고·판매의 모든 비즈니스 상태 전이를 불변 레코드로 기록한다.
 * {@code flowCode}를 공통 키로 사용하여 동일한 흐름의 이력을 시계열로 조회할 수 있다.
 *
 * *주요 필드:
 *
 *   - {@code sourceType} — 이력의 출처 유형 (주문/재고/판매 등)
 *   - {@code phase} — 전이된 비즈니스 단계 ({@link BusinessPhase})
 *   - {@code kind} — 변경 종류 (생성/수정/삭제 등, {@link Kind})
 *   - {@code fromValue} / {@code toValue} — 전이 전후 상태 이름
 * 
 * 
 */
@Getter
@Table(name = "STATUS_HISTORY", indexes = {
        @Index(name = "idx_hist_flowcode_created", columnList = "FLOW_CODE, CREATED_AT")
})
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StatusHistory {
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "FLOW_CODE", nullable = false)
    private Long flowCode;

    @Enumerated(EnumType.STRING) @Column(name = "SOURCE_TYPE")
    private SourceType sourceType;

    @Enumerated(EnumType.STRING) @Column(name = "PHASE")
    private BusinessPhase phase;

    @Enumerated(EnumType.STRING) @Column(name = "KIND")
    private Kind kind;
    @Column(name = "FROM_VALUE")
    private String fromValue;
    @Column(name = "TO_VALUE")
    private String toValue;
    @Column(name = "CONTENT", columnDefinition = "TEXT")
    private String content;
    @Column(name = "CREATED_AT")
    private OffsetDateTime createAt;
    @Column(name = "USER_NAME")
    private String userName;

    @PrePersist
    void prePersist() {
        if (createAt == null) createAt = OffsetDateTime.now(KST);
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
