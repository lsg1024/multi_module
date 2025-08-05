package com.msa.order.local.domain.order.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * createAt은 기본적으로 설정된 값과 사용자 설정 값이 존재하며 우선순위는 사용자 설정 값에 있다
 */
@Table(name = "STATUS_HISTORY")
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StatusHistory {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;
    @Column(name = "ORDER_STATUS", nullable = false)
    @Enumerated(EnumType.STRING)
    private OrderStatus orderStatus;
    @Column(name = "CREATE_AT")
    private String createAt;
    @Column(name = "USER_NAME")
    private String userName;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ORDER_ID")
    private Orders order;

    @Builder
    public StatusHistory(OrderStatus orderStatus, String createAt, String userName, Orders order) {
        this.orderStatus = orderStatus;
        this.createAt = createAt;
        this.userName = userName;
        this.order = order;
    }

    public void setOrder(Orders orders) {
        this.order = orders;
    }
}
