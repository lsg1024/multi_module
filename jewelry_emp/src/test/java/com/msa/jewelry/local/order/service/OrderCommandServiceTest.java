package com.msa.jewelry.local.order.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * OrderCommandService 단위 테스트 placeholder.
 * <p>
 * 모놀리스 통합으로 OrderCommandService 와 OrderProcessingService 를 한 클래스로 합치면서
 * 이전의 Mockito 기반 위임 검증 테스트는 더 이상 구조에 맞지 않는다.
 * 실제 동작 검증은 OrdersServiceIntegrationTest 의 @SpringBootTest 흐름에서 수행된다.
 */
@DisplayName("OrderCommandService 단위 테스트 (placeholder)")
class OrderCommandServiceTest {

    @Test
    @DisplayName("클래스 로드 가능 여부 sanity check")
    void 로드() {
        assertThat(OrderCommandService.class).isNotNull();
    }
}
