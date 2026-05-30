package com.msa.jewelry.local.transaction_history.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SaleLogService 단위 테스트 (placeholder)")
class SaleLogServiceTest {

    @Test
    @DisplayName("default 생성자로 인스턴스화 가능")
    void 인스턴스화() {
        SaleLogService service = new SaleLogService();

        assertThat(service).isNotNull();
    }

    @Test
    @DisplayName("@Service 어노테이션 — Spring 빈 등록 보장")
    void Service_어노테이션() {
        assertThat(SaleLogService.class.isAnnotationPresent(Service.class)).isTrue();
    }

    @Test
    @DisplayName("@Transactional 어노테이션 — 모든 public 메서드 트랜잭션 경계 보장")
    void Transactional_어노테이션() {
        assertThat(SaleLogService.class.isAnnotationPresent(Transactional.class)).isTrue();
    }
}
