package com.msa.order.local.domain.order.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.time.ZoneId;

@Getter
@Table(name = "STATUS_HISTORY", indexes = {
        @Index(name = "idx_hist_flowcode_created", columnList = "FLOW_CODE, CREATED_AT")
})
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StatusHistory {
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    public enum SourceType { ORDER, FIX, NORMAL }
    public enum BusinessPhase { ORDER, WAITING, ORDER_FAIL, STOCK, STOCK_FAIL, FIX, NORMAL, RENTAL, RETURN, SALE, DELETE}
    public enum Kind { CREATE, UPDATE, DELETE, RESTORE, EXPECT }

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
    @Column(name = "CREATED_AT")
    private OffsetDateTime createAt;
    @Column(name = "USER_NAME")
    private String userName;

    @PrePersist
    void prePersist() {
        if (createAt == null) createAt = OffsetDateTime.now(KST);
    }

    public static StatusHistory create(Long flowCode, SourceType src,
                                       BusinessPhase to, Kind kind, String userName) {
        StatusHistory h = new StatusHistory();
        h.flowCode = flowCode;
        h.sourceType = src;
        h.phase = to;
        h.fromValue = null;
        h.toValue = to.name();
        h.kind = kind;
        h.userName = userName;
        return h;
    }
    public static StatusHistory phaseChange(Long flowCode, SourceType src,
                                            BusinessPhase from, BusinessPhase to, String userName) {
        StatusHistory h = base(flowCode, src, Kind.UPDATE, userName);
        h.phase = to;
        h.fromValue = from != null ? from.name() : null;
        h.toValue = to.name();
        return h;
    }

    public static StatusHistory fieldChange(Long flowCode, SourceType src,
                                            BusinessPhase phase, String from, String to,
                                             String userName) {
        StatusHistory h = base(flowCode, src, Kind.UPDATE, userName);
        h.phase = phase;
        h.fromValue = from;
        h.toValue = to;
        return h;
    }


    protected static StatusHistory base(Long flow, SourceType src, Kind kind, String userName) {
        StatusHistory h = new StatusHistory();
        h.flowCode = flow; h.sourceType = src; h.kind = kind; h.userName = userName;
        return h;
    }
}
